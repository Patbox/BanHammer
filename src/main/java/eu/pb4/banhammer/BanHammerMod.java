package eu.pb4.banhammer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import eu.pb4.banhammer.commands.GeneralCommands;
import eu.pb4.banhammer.commands.PunishCommands;
import eu.pb4.banhammer.commands.UnpunishCommands;
import eu.pb4.banhammer.config.ConfigData;
import eu.pb4.banhammer.config.ConfigManager;
import eu.pb4.banhammer.database.DatabaseHandlerInterface;
import eu.pb4.banhammer.database.MySQLDatabase;
import eu.pb4.banhammer.database.SQLiteDatabase;
import eu.pb4.banhammer.types.BasicPunishment;
import eu.pb4.banhammer.types.PunishmentTypes;
import eu.pb4.banhammer.types.SyncedPunishment;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class BanHammerMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("BanHammer");
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	public static final MiniMessage miniMessage = MiniMessage.get();
	public static String VERSION = FabricLoader.getInstance().getModContainer("banhammer").get().getMetadata().getVersion().getFriendlyString();
	private static FabricServerAudiences AUDIENCE;
	private static MinecraftServer SERVER;

	public static DatabaseHandlerInterface DATABASE;

	public static ConcurrentHashMap<String, String> IP_CACHE = null;

	@Override
	public void onInitialize() {

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			AUDIENCE = FabricServerAudiences.of(server);
			SERVER = server;
			boolean loaded = ConfigManager.loadConfig();

			File ipcacheFile = Paths.get("ipcache.json").toFile();

			try {
				IP_CACHE = ipcacheFile.exists() ? GSON.fromJson(new FileReader(ipcacheFile), new TypeToken<ConcurrentHashMap<String, String>>() {}.getType()) : new ConcurrentHashMap<>();
			} catch (FileNotFoundException e) {
				LOGGER.warn("Couldn't load ipcache.json! Creating new one...");
				IP_CACHE = new ConcurrentHashMap<>();
			}

			if (loaded) {
				ConfigData configData = ConfigManager.getConfig().configData;

				try {
					switch (configData.databaseType.toLowerCase(Locale.ROOT)) {
						case "sqlite":
							DATABASE = new SQLiteDatabase(configData.sqliteDatabaseLocation);
							break;
						case "mysql":
							DATABASE = new MySQLDatabase(configData.mysqlDatabaseAddress, configData.mysqlDatabaseName, configData.mysqlDatabaseUsername, configData.mysqlDatabasePassword);
							break;
						default:
							LOGGER.error("Config file is invalid (database)! Stopping server...");
							server.stop(true);
					}
				} catch (SQLException e) {
					e.printStackTrace();

					LOGGER.error("Couldn't connect to database! Stopping server...");
					server.stop(true);
				}
			} else {
				LOGGER.error("Config file is invalid! Stopping server...");
				server.stop(true);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			if (DATABASE != null) {
				DATABASE.closeConnection();
			}
			SERVER = null;
			AUDIENCE = null;
			DATABASE = null;

			File ipcacheFile = Paths.get("ipcache.json").toFile();

			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(ipcacheFile));
				writer.write(GSON.toJson(IP_CACHE));
				writer.close();
			} catch (IOException exception) {
				exception.printStackTrace();
			}
		});

		PunishCommands.register();
		UnpunishCommands.register();
		GeneralCommands.register();

	}

	public static FabricServerAudiences getAdventure() {
		return AUDIENCE;
	}

	public static void punishPlayer(BasicPunishment punishment, boolean silent) {
		CompletableFuture.runAsync(() -> {
			if (punishment.getType().databaseName != null) {
				DATABASE.insertPunishment(punishment);
			}

			if (ConfigManager.getConfig().configData.storeAllPunishmentsInHistory) {
				DATABASE.insertPunishmentIntoHistory(punishment);
			}
		});

		if (punishment.getType().kick && punishment.getType().ipBased) {
			for (ServerPlayerEntity player : SERVER.getPlayerManager().getPlayerList()) {
				if (player.getIp().equals(punishment.getIPofPlayer())) {
					player.networkHandler.disconnect(punishment.getDisconnectMessage());
				}
			}
		} else if (punishment.getType().kick) {
			ServerPlayerEntity player = SERVER.getPlayerManager().getPlayer(punishment.getUUIDofPlayer());

			if (player != null) {
				player.networkHandler.disconnect(punishment.getDisconnectMessage());
			}
		}

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
		PUNISHMENT_EVENT.invoker().onPunishment(punishment);
	}

	public static void removePunishment(String id, PunishmentTypes type) {
		DATABASE.removePunishment(id, type);
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

	public static final Event<BanHammerMod.PunishmentEvent> PUNISHMENT_EVENT = EventFactory.createArrayBacked(PunishmentEvent.class, (callbacks) -> (punishment) -> {
		for(PunishmentEvent callback : callbacks ) {
			callback.onPunishment(punishment);
		}
	});

	@FunctionalInterface
	public interface PunishmentEvent {
		void onPunishment(BasicPunishment punishment);
	}
}
