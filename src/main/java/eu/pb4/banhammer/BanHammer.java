package eu.pb4.banhammer;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import eu.pb4.banhammer.commands.GeneralCommands;
import eu.pb4.banhammer.commands.PunishCommands;
import eu.pb4.banhammer.commands.UnpunishCommands;
import eu.pb4.banhammer.config.Config;
import eu.pb4.banhammer.config.data.ConfigData;
import eu.pb4.banhammer.config.ConfigManager;
import eu.pb4.banhammer.config.data.DiscordMessageData;
import eu.pb4.banhammer.database.DatabaseHandlerInterface;
import eu.pb4.banhammer.database.MySQLDatabase;
import eu.pb4.banhammer.database.SQLiteDatabase;
import eu.pb4.banhammer.imports.VanillaImport;
import eu.pb4.banhammer.types.BasicPunishment;
import eu.pb4.banhammer.types.PunishmentTypes;
import eu.pb4.banhammer.types.SyncedPunishment;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class BanHammer implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("BanHammer");
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	public static String VERSION = FabricLoader.getInstance().getModContainer("banhammer").get().getMetadata().getVersion().getFriendlyString();
	public static MinecraftServer SERVER;

	public static DatabaseHandlerInterface DATABASE;

	public static ConcurrentHashMap<UUID, String> UUID_TO_IP_CACHE = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, UUID> IP_TO_UUID_CACHE = new ConcurrentHashMap<>();

	public static HashMap<String, PunishmentImporter> IMPORTERS = new HashMap<>();

	private void onServerStarting(MinecraftServer server) {
		this.crabboardDetection();
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
							IP_TO_UUID_CACHE.put(ip, uuid);
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
		this.crabboardDetection();
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
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

		PunishCommands.register();
		UnpunishCommands.register();
		GeneralCommands.register();

	}

	public static void punishPlayer(BasicPunishment punishment, boolean silent) {
		punishPlayer(punishment, silent, false);
	}

	public static void punishPlayer(BasicPunishment punishment, boolean silent, boolean invisible) {
		Config config = ConfigManager.getConfig();

		CompletableFuture.runAsync(() -> {
			if (punishment.type.databaseName != null) {
				DATABASE.insertPunishment(punishment);
			}

			if (ConfigManager.getConfig().configData.storeAllPunishmentsInHistory) {
				DATABASE.insertPunishmentIntoHistory(punishment);
			}

			if (config.webhook != null) {
				DiscordMessageData.Message message = punishment.getRawDiscordMessage();

				config.webhook.send(message.build(punishment.getStringPlaceholders()));
			}
		});


		if (punishment.type.kick && punishment.type.ipBased) {
			boolean alreadyStandardBanned = false;

			for (ServerPlayerEntity player : SERVER.getPlayerManager().getPlayerList()) {
				if (player.getIp().equals(punishment.bannedIP)) {
					player.networkHandler.disconnect(punishment.getDisconnectMessage());
					if (ConfigManager.getConfig().configData.standardBanPlayersWithBannedIps && punishment.type == PunishmentTypes.IPBAN) {
						BasicPunishment punishment1 = new BasicPunishment(player.getUuid(), player.getIp(), player.getDisplayName(), player.getGameProfile().getName(),
								punishment.adminUUID,
								punishment.adminDisplayName,
								punishment.time,
								punishment.duration,
								punishment.reason,
								PunishmentTypes.BAN);

						if (player.getUuid() == punishment.bannedUUID) {
							alreadyStandardBanned = true;
						}

						punishPlayer(punishment1, true, true);
					}
				}
			}

			if (ConfigManager.getConfig().configData.standardBanPlayersWithBannedIps && punishment.type == PunishmentTypes.IPBAN && !alreadyStandardBanned) {
				BasicPunishment punishment1 = new BasicPunishment(punishment.bannedUUID, punishment.bannedIP, punishment.bannedDisplayName, punishment.bannedName,
						punishment.adminUUID,
						punishment.adminDisplayName,
						punishment.time,
						punishment.duration,
						punishment.reason,
						PunishmentTypes.BAN);

				punishPlayer(punishment1, true, true);
			}
		} else if (punishment.type.kick) {
			ServerPlayerEntity player = SERVER.getPlayerManager().getPlayer(punishment.bannedUUID);

			if (player != null) {
				player.networkHandler.disconnect(punishment.getDisconnectMessage());
			}
		}

		if (!invisible) {
			if (!silent) {
				SERVER.getPlayerManager().broadcastChatMessage(punishment.getChatMessage(), MessageType.SYSTEM, Util.NIL_UUID);
			} else {
				Text message = punishment.getChatMessage();

				SERVER.sendSystemMessage(message, Util.NIL_UUID);

				for (ServerPlayerEntity player : SERVER.getPlayerManager().getPlayerList()) {
					if (Permissions.check(player, "banhammer.seesilent", 1)) {
						player.sendMessage(message, MessageType.SYSTEM, Util.NIL_UUID);
					}
				}
			}
		}

		PUNISHMENT_EVENT.invoker().onPunishment(punishment, silent, invisible);
	}

	public static int removePunishment(String id, PunishmentTypes type) {
		return DATABASE.removePunishment(id, type);
	}

	public static List<SyncedPunishment> getPlayersPunishments(String id, PunishmentTypes type) {
		List<SyncedPunishment> punishments = DATABASE.getPunishments(id, type);

		List<SyncedPunishment> out = new LinkedList<>();
		for (SyncedPunishment punishment : punishments) {
			if (punishment.isExpired()) {
				DATABASE.removePunishment(punishment.getId(), type);
			} else {
				out.add(punishment);
			}
		}

		return out;
	}

	public static boolean isPlayerPunished(String id, PunishmentTypes type) {
		List<SyncedPunishment> punishments = DATABASE.getPunishments(id, type);
		for (SyncedPunishment punishment : punishments) {
			if (punishment.isExpired()) {
				DATABASE.removePunishment(punishment.getId(), type);
			} else {
				return true;
			}
		}

		return false;
	}

	public static final Event<BanHammer.PunishmentEvent> PUNISHMENT_EVENT = EventFactory.createArrayBacked(PunishmentEvent.class, (callbacks) -> (punishment, s, i) -> {
		for(PunishmentEvent callback : callbacks ) {
			callback.onPunishment(punishment, s, i);
		}
	});

	@FunctionalInterface
	public interface PunishmentEvent {
		void onPunishment(BasicPunishment punishment, boolean silent, boolean invisible);
	}

	@FunctionalInterface
	public interface PunishmentImporter {
		boolean importPunishments(boolean remove);
	}

	private void crabboardDetection() {
		if (FabricLoader.getInstance().isModLoaded("cardboard")) {
			LOGGER.error("");
			LOGGER.error("Cardboard detected! This mod doesn't work with it!");
			LOGGER.error("You won't get any support as long as it's present!");
			LOGGER.error("");
			LOGGER.error("Read more: https://gist.github.com/Patbox/e44844294c358b614d347d369b0fc3bf");
			LOGGER.error("");
		}
	}
}
