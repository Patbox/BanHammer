package eu.pb4.banhammer.impl;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import eu.pb4.banhammer.api.BanHammer;
import eu.pb4.banhammer.api.PunishmentData;
import eu.pb4.banhammer.api.PunishmentType;
import eu.pb4.banhammer.api.TriState;
import eu.pb4.banhammer.impl.commands.GeneralCommands;
import eu.pb4.banhammer.impl.commands.PunishCommands;
import eu.pb4.banhammer.impl.commands.UnpunishCommands;
import eu.pb4.banhammer.impl.config.Config;
import eu.pb4.banhammer.impl.config.ConfigManager;
import eu.pb4.banhammer.impl.gson.CodecSerializer;
import eu.pb4.banhammer.impl.gson.LowercaseEnumTypeAdapterFactory;
import eu.pb4.banhammer.impl.database.DatabaseHandlerInterface;
import eu.pb4.banhammer.impl.database.MySQLDatabase;
import eu.pb4.banhammer.impl.database.PostgreSQLDatabase;
import eu.pb4.banhammer.impl.database.SQLiteDatabase;
import eu.pb4.banhammer.impl.importers.BanHammerJsonImporter;
import eu.pb4.banhammer.impl.importers.VanillaImport;
import eu.pb4.placeholders.api.PlaceholderContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class BanHammerImpl implements ModInitializer {
    public static final int LOWER_LIMIT = 32;
    public static final int UPPER_LIMIT = LOWER_LIMIT * 2;
    public static final List<PunishmentData> CACHED_PUNISHMENTS = new CopyOnWriteArrayList<>();
    public static final Logger LOGGER = LogManager.getLogger("BanHammer");
    public static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static final Event<BanHammer.PunishmentEvent> PUNISHMENT_EVENT = EventFactory.createArrayBacked(BanHammer.PunishmentEvent.class, (callbacks) -> (punishment, s, i) -> {
        for (var callback : callbacks) {
            callback.onPunishment(punishment, s, i);
        }
    });

    public static final Event<BanHammer.PunishmentCheckEvent> CAN_PUNISH_CHECK_EVENT = EventFactory.createArrayBacked(BanHammer.PunishmentCheckEvent.class, (callbacks) -> (gameProfile, source) -> {
        for (var callback : callbacks) {
            var state = callback.canSourcePunish(gameProfile, source);

            if (state != TriState.DEFAULT) {
                return state;
            }
        }

        return TriState.TRUE;
    });
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
            .registerTypeHierarchyAdapter(Component.class, CodecSerializer.TEXT)
            .registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
            .create();
    public static MinecraftServer SERVER;
    public static DatabaseHandlerInterface DATABASE;
    public static ConcurrentHashMap<UUID, String> UUID_TO_IP_CACHE = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Set<UUID>> IP_TO_UUID_CACHE = new ConcurrentHashMap<>();
    public static HashMap<String, BanHammer.PunishmentImporter> IMPORTERS = new HashMap<>();

    public static void punishPlayer(PunishmentData punishment, boolean silent) {
        punishPlayer(punishment, silent, false);
    }

    public static void punishPlayer(PunishmentData punishment, boolean silent, boolean invisible) {
        Config config = ConfigManager.getConfig();
        if (config.configData.cachePunishmentsLocally && punishment.type.databaseName != null) {
            CACHED_PUNISHMENTS.add(punishment);
            if (CACHED_PUNISHMENTS.size() > UPPER_LIMIT) {
                var dynInt = new int[]{0};
                CACHED_PUNISHMENTS.removeIf(x -> dynInt[0]++ < LOWER_LIMIT || x.isExpired());
            }
        }

        CompletableFuture.runAsync(() -> {
            if (punishment.type.databaseName != null) {
                DATABASE.insertPunishment(punishment);
            }

            if (ConfigManager.getConfig().configData.storeAllPunishmentsInHistory) {
                DATABASE.insertPunishmentIntoHistory(punishment);
            }

            if (!invisible && !config.webhooks.isEmpty()) {
                var json = HttpRequest.BodyPublishers.ofString(punishment.getRawDiscordMessage().build(punishment.getStringPlaceholders()));

                for (var hook : config.webhooks) {
                    HTTP_CLIENT.sendAsync(HttpRequest.newBuilder()
                            .uri(hook)
                            .headers("Content-Type", "application/json")
                            .POST(json).build(), HttpResponse.BodyHandlers.discarding());
                }
            }

            if (punishment.type == PunishmentType.WARN) {
                int count = getPlayersPunishments(punishment.playerUUID.toString(), PunishmentType.WARN).size() + 1;

                int distance = Integer.MAX_VALUE;
                List<String> actions = null;

                for (var act : config.configData.warnActions) {
                    int dist2 = count - act.count;
                    if (act.count <= count && distance > dist2) {
                        distance = dist2;
                        actions = act.execute;
                    }
                }

                if (actions != null) {
                    List<String> finalActions = actions;
                    SERVER.execute(() -> {
                        for (var i : finalActions) {
                            SERVER.getCommands().performPrefixedCommand(SERVER.createCommandSourceStack(),
                                    i
                                            .replace("${uuid}", punishment.playerUUID.toString())
                                            .replace("${name}", punishment.playerName)
                                            .replace("${ip}", punishment.playerIP)
                                            .replace("${reason}", punishment.reason)
                                            .replace("${count}", "" + count)
                            );
                        }
                    });
                }
            }
        });

        if (punishment.type.kick && punishment.type.ipBased) {
            boolean alreadyStandardBanned = false;

            for (ServerPlayer player : SERVER.getPlayerList().getPlayers()) {
                if (player.getIpAddress().equals(punishment.playerIP)) {
                    player.connection.disconnect(punishment.getDisconnectMessage(PlaceholderContext.of(player)));
                    if (ConfigManager.getConfig().configData.standardBanPlayersWithBannedIps && punishment.type == PunishmentType.IP_BAN) {
                        PunishmentData punishment1 = new PunishmentData(player.getUUID(), player.getIpAddress(), player.getDisplayName(), player.getGameProfile().name(),
                                punishment.adminUUID,
                                punishment.adminDisplayName,
                                punishment.time,
                                punishment.duration,
                                punishment.reason,
                                PunishmentType.BAN);

                        if (player.getUUID() == punishment.playerUUID) {
                            alreadyStandardBanned = true;
                        }

                        punishPlayer(punishment1, true, true);
                    }
                }
            }

            if (ConfigManager.getConfig().configData.standardBanPlayersWithBannedIps && punishment.type == PunishmentType.IP_BAN && !alreadyStandardBanned) {
                PunishmentData punishment1 = new PunishmentData(punishment.playerUUID, punishment.playerIP, punishment.playerDisplayName, punishment.playerName,
                        punishment.adminUUID,
                        punishment.adminDisplayName,
                        punishment.time,
                        punishment.duration,
                        punishment.reason,
                        PunishmentType.BAN);

                punishPlayer(punishment1, true, true);
            }
        } else if (punishment.type.kick) {
            ServerPlayer player = SERVER.getPlayerList().getPlayer(punishment.playerUUID);

            if (player != null) {
                player.connection.disconnect(punishment.getDisconnectMessage(PlaceholderContext.of(player)));
            }
        }

        if (!invisible) {
            if (!silent) {
                SERVER.getPlayerList().broadcastSystemMessage(punishment.getChatMessage(PlaceholderContext.of(new GameProfile(punishment.playerUUID, punishment.playerName), SERVER)), false);
            } else {
                Component message = punishment.getChatMessage(PlaceholderContext.of(new GameProfile(punishment.playerUUID, punishment.playerName), SERVER));

                SERVER.sendSystemMessage(message);

                var punishedPlayer = SERVER.getPlayerList().getPlayer(punishment.playerUUID);
                if (punishedPlayer != null) {
                    punishedPlayer.sendSystemMessage(message);
                }

                for (ServerPlayer player : SERVER.getPlayerList().getPlayers()) {
                    if (player != punishedPlayer && Permissions.check(player.createCommandSourceStack(), "banhammer.seesilent", 3)) {
                        player.sendSystemMessage(message);
                    }
                }
            }
        }

        PUNISHMENT_EVENT.invoker().onPunishment(punishment, silent, invisible);
    }

    public static int removePunishment(String id, PunishmentType type) {
        CACHED_PUNISHMENTS.removeIf(x -> (x.type == type) && (x.type.ipBased ? x.playerIP.equals(id) : x.playerUUID.toString().equals(id)));

        return DATABASE.removePunishment(id, type);
    }

    public static int removePunishment(PunishmentData.Synced punishment) {
        return DATABASE.removePunishment(punishment.getId(), punishment.type);
    }

    public static List<PunishmentData.Synced> getPlayersPunishments(String id, PunishmentType type) {
        final List<PunishmentData.Synced> punishments = new ArrayList<>();

        Consumer<PunishmentData.Synced> consumer = (punishment) -> {
            if (punishment.isExpired()) {
                DATABASE.removePunishment(punishment.getId(), punishment.type);
            } else {
                punishments.add(punishment);
            }
        };

        if (type != null) {
            DATABASE.getPunishments(id, type, consumer);
        } else {
            for (var type2 : PunishmentType.values()) {
                DATABASE.getPunishments(id, type2, consumer);
            }
        }
        return punishments;
    }

    public static boolean isPlayerPunished(String id, PunishmentType type) {
        List<PunishmentData.Synced> punishments = new ArrayList<>();
        DATABASE.getPunishments(id, type, punishments::add);
        for (PunishmentData.Synced punishment : punishments) {
            if (punishment.isExpired()) {
                DATABASE.removePunishment(punishment.getId(), type);
            } else {
                return true;
            }
        }

        return false;
    }

    public static void addPunishment(PunishmentData punishment) {
        if (punishment.isExpired()) {
            CompletableFuture.runAsync(() -> {
                if (ConfigManager.getConfig().configData.storeAllPunishmentsInHistory) {
                    DATABASE.insertPunishmentIntoHistory(punishment);
                }
            });
        } else {
            punishPlayer(punishment,true, true);
        }
    }

    private void onServerStarting(MinecraftServer server) {
        CardboardWarning.checkAndAnnounce();
        SERVER = server;
        boolean loaded = ConfigManager.loadConfig();

        File ipCacheFile = Paths.get("ipcache.json").toFile();

        try {
            ConcurrentHashMap<String, String> ipCache = ipCacheFile.exists() ? GSON.fromJson(new FileReader(ipCacheFile), new TypeToken<ConcurrentHashMap<String, String>>() {
            }.getType()) : null;


            if (ipCache != null) {
                for (var entry : ipCache.entrySet()) {
                    try {
                        var uuid = UUID.fromString(entry.getKey());
                        var ip = entry.getValue();

                        if (uuid != null && ip != null) {
                            UUID_TO_IP_CACHE.put(uuid, ip);
                            IP_TO_UUID_CACHE.computeIfAbsent(ip, (x) -> new HashSet<>()).add(uuid);
                        }
                    } catch (Exception e) {
                        // Silence!
                    }
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.warn("Couldn't load ipcache.json! Creating new one...");
        }

        if (loaded) {
            var config = ConfigManager.getConfig();

            try {
                switch (config.configData.databaseType.toLowerCase(Locale.ROOT)) {
                    case "sqlite" -> DATABASE = new SQLiteDatabase(config.configData.sqliteDatabaseLocation);
                    case "mysql" -> {
                        var dbConfig = config.getDatabaseConfig("mysql");
                        DATABASE = new MySQLDatabase(dbConfig.address, dbConfig.database, dbConfig.username, dbConfig.password, config.configData.databaseArgs);
                    }
                    case "postgresql", "postgres" -> {
                        var dbConfig = config.getDatabaseConfig("postgresql");
                        DATABASE = new PostgreSQLDatabase(dbConfig.address, dbConfig.database, dbConfig.username, dbConfig.password, config.configData.databaseArgs);
                    }
                    default -> {
                        LOGGER.error("Config file is invalid (database)! Stopping server...");
                        server.stopServer();
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();

                LOGGER.error("Couldn't connect to database! Stopping server...");
                server.stopServer();
                return;
            }

            IMPORTERS.put("vanilla", new VanillaImport());
            IMPORTERS.put("banhammer_export", new BanHammerJsonImporter());

            LOGGER.info("BanHammer connected successfully to " + DATABASE.name() + " database!");
        } else {
            LOGGER.error("Config file is invalid! Stopping server...");
            server.stopServer();
        }

    }

    @Override
    public void onInitialize() {
        CardboardWarning.checkAndAnnounce();
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        GenericModInfo.build(FabricLoader.getInstance().getModContainer("banhammer").get());

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (DATABASE != null) {
                DATABASE.closeConnection();
            }
            SERVER = null;
            DATABASE = null;

            File ipcacheFile = Paths.get("ipcache.json").toFile();

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(ipcacheFile));

                var ipCache = new HashMap<String, String>();

                for (var entry : UUID_TO_IP_CACHE.entrySet()) {
                    ipCache.put(entry.getKey().toString(), entry.getValue());
                }

                writer.write(GSON.toJson(ipCache));
                writer.close();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            var punishments = getPlayersPunishments(sender.getStringUUID(), PunishmentType.MUTE);
            if (!punishments.isEmpty()) {
                var punishment = punishments.getFirst();
                sender.displayClientMessage(punishment.getDisconnectMessage(PlaceholderContext.of(sender)), false);
                return false;
            }
            return true;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!ConfigManager.getConfig().configData.showAssociatedAccounts) {
                return;
            }

            String ip = handler.player.getIpAddress();
            Set<UUID> associatedAccounts = IP_TO_UUID_CACHE.get(ip);
            if (associatedAccounts == null || associatedAccounts.size() <= 1) return;
            List<Component> playerMessages = new LinkedList<>();

            for (UUID player : associatedAccounts) {
                String name = SERVER.services().nameToIdCache().get(player).map(NameAndId::name).orElse(player.toString());
                MutableComponent text = Component.literal("[" + name + "]");

                List<PunishmentData.Synced> punishments = getPlayersPunishments(player.toString(), PunishmentType.BAN);
                playerMessages.add(text);
                if (SERVER.getPlayerList().getPlayer(player) != null || player.equals(handler.player.getUUID())) {
                    text.withStyle(ChatFormatting.GREEN);
                } else if (punishments.isEmpty()) {
                    text.withStyle(ChatFormatting.GRAY);
                } else {
                    text.withStyle(ChatFormatting.RED);
                    PunishmentData.Synced punishment = punishments.getFirst();

                    text.withStyle(style ->
                        style.withHoverEvent(new HoverEvent.ShowText(punishment.getChatMessage(PlaceholderContext.of(new GameProfile(punishment.playerUUID, punishment.playerName), SERVER))))
                    );
                }
            }

            Component message = ComponentUtils.formatList(playerMessages, Component.literal(" "));

            SERVER.sendSystemMessage(message);

            for (ServerPlayer player : SERVER.getPlayerList().getPlayers()) {
                if (Permissions.check(player.createCommandSourceStack(), "banhammer.seeassociated", 3)) {
                    player.sendSystemMessage(message);
                }
            }

        });

        PunishCommands.register();
        UnpunishCommands.register();
        GeneralCommands.register();

    }

    public static final class IpCacheFile {
        public int version = 1;
        public Map<String, String> data = new HashMap<>();
    }
}
