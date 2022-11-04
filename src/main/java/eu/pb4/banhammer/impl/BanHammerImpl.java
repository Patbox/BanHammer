package eu.pb4.banhammer.impl;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import eu.pb4.banhammer.api.BanHammer;
import eu.pb4.banhammer.api.PunishmentData;
import eu.pb4.banhammer.api.PunishmentType;
import eu.pb4.banhammer.api.TriState;
import eu.pb4.banhammer.impl.commands.GeneralCommands;
import eu.pb4.banhammer.impl.commands.PunishCommands;
import eu.pb4.banhammer.impl.commands.UnpunishCommands;
import eu.pb4.banhammer.impl.config.Config;
import eu.pb4.banhammer.impl.config.ConfigManager;
import eu.pb4.banhammer.impl.config.data.ConfigData;
import eu.pb4.banhammer.impl.config.data.DiscordMessageData;
import eu.pb4.banhammer.impl.database.DatabaseHandlerInterface;
import eu.pb4.banhammer.impl.database.MySQLDatabase;
import eu.pb4.banhammer.impl.database.SQLiteDatabase;
import eu.pb4.banhammer.impl.importers.VanillaImport;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class BanHammerImpl implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("BanHammer");
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
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
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

        CompletableFuture.runAsync(() -> {
            if (punishment.type.databaseName != null) {
                DATABASE.insertPunishment(punishment);
            }

            if (ConfigManager.getConfig().configData.storeAllPunishmentsInHistory) {
                DATABASE.insertPunishmentIntoHistory(punishment);
            }

            if (!config.webhooks.isEmpty()) {
                DiscordMessageData.Message message = punishment.getRawDiscordMessage();
                var out = message.build(punishment.getStringPlaceholders());
                for (var hook : config.webhooks) {
                    hook.send(out);
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
                            SERVER.getCommandManager().executeWithPrefix(SERVER.getCommandSource(),
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

            for (ServerPlayerEntity player : SERVER.getPlayerManager().getPlayerList()) {
                if (player.getIp().equals(punishment.playerIP)) {
                    player.networkHandler.disconnect(punishment.getDisconnectMessage());
                    if (ConfigManager.getConfig().configData.standardBanPlayersWithBannedIps && punishment.type == PunishmentType.IP_BAN) {
                        PunishmentData punishment1 = new PunishmentData(player.getUuid(), player.getIp(), player.getDisplayName(), player.getGameProfile().getName(),
                                punishment.adminUUID,
                                punishment.adminDisplayName,
                                punishment.time,
                                punishment.duration,
                                punishment.reason,
                                PunishmentType.BAN);

                        if (player.getUuid() == punishment.playerUUID) {
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
            ServerPlayerEntity player = SERVER.getPlayerManager().getPlayer(punishment.playerUUID);

            if (player != null) {
                player.networkHandler.disconnect(punishment.getDisconnectMessage());
            }
        }

        if (!invisible) {
            if (!silent) {
                SERVER.getPlayerManager().broadcast(punishment.getChatMessage(), false);
            } else {
                Text message = punishment.getChatMessage();

                SERVER.sendMessage(message);

                for (ServerPlayerEntity player : SERVER.getPlayerManager().getPlayerList()) {
                    if (Permissions.check(player, "banhammer.seesilent", 1)) {
                        player.sendMessage(message);
                    }
                }
            }
        }

        PUNISHMENT_EVENT.invoker().onPunishment(punishment, silent, invisible);
    }

    public static int removePunishment(String id, PunishmentType type) {
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
            ConfigData configData = ConfigManager.getConfig().configData;

            try {
                switch (configData.databaseType.toLowerCase(Locale.ROOT)) {
                    case "sqlite" -> DATABASE = new SQLiteDatabase(configData.sqliteDatabaseLocation);
                    case "mysql" -> DATABASE = new MySQLDatabase(configData.mysqlDatabaseAddress, configData.mysqlDatabaseName, configData.mysqlDatabaseUsername, configData.mysqlDatabasePassword, configData.mysqlDatabaseArgs);
                    default -> {
                        LOGGER.error("Config file is invalid (database)! Stopping server...");
                        server.stop(true);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();

                LOGGER.error("Couldn't connect to database! Stopping server...");
                server.stop(true);
            }

            IMPORTERS.put("vanilla", new VanillaImport());
        } else {
            LOGGER.error("Config file is invalid! Stopping server...");
            server.stop(true);
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
            var punishments = getPlayersPunishments(sender.getUuidAsString(), PunishmentType.MUTE);
            if (punishments.size() > 0) {
                var punishment = punishments.get(0);
                sender.sendMessage(punishment.getDisconnectMessage(), false);
                return false;
            }
            return true;
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
