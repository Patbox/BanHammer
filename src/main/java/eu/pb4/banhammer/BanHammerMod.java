package eu.pb4.banhammer;

import eu.pb4.banhammer.commands.Commands;
import eu.pb4.banhammer.config.ConfigManager;
import eu.pb4.banhammer.database.DatabaseHandlerInterface;
import eu.pb4.banhammer.database.SQLiteDatabase;
import eu.pb4.banhammer.types.BasicPunishment;
import eu.pb4.banhammer.types.PunishmentTypes;
import eu.pb4.banhammer.types.SyncedPunishment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class BanHammerMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("BanHammer");

	public static final MiniMessage miniMessage = MiniMessage.get();
	public static String VERSION = FabricLoader.getInstance().getModContainer("banhammer").get().getMetadata().getVersion().getFriendlyString();
	private static FabricServerAudiences AUDIENCE;
	private static MinecraftServer SERVER;

	public static DatabaseHandlerInterface DATABASE;

	public static HashMap<String, String> IPCACHE = new HashMap();

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			AUDIENCE = FabricServerAudiences.of(server);
			SERVER = server;
			DATABASE = new SQLiteDatabase();
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			DATABASE.closeConnection();
			SERVER = null;
			AUDIENCE = null;
			DATABASE = null;
		});

		ConfigManager.loadConfig();
		Commands.register();
	}

	public static FabricServerAudiences getAdventure() {
		return AUDIENCE;
	}

	public static void punishPlayer(BasicPunishment punishment) {
		DATABASE.insertPunishment(punishment);

		if (ConfigManager.getConfig().getConfigData().storeAllPunishmentsInHistory) {
			DATABASE.insertPunishmentIntoHistory(punishment);
		}

		if (punishment.getType().kick && punishment.getType().ipBased) {
			for (ServerPlayerEntity player : SERVER.getPlayerManager().getPlayerList()) {
				if (player.getIp().equals(punishment.getIPofPlayer())) {
					player.networkHandler.disconnect(Helpers.parseMessage(ConfigManager.getConfig().getBanScreenMessage(), Helpers.getTemplateFor(punishment)));
				}
			}
		} else if (punishment.getType().kick) {
			ServerPlayerEntity player = SERVER.getPlayerManager().getPlayer(punishment.getUUIDofPlayer());

			if (player != null) {
				player.networkHandler.disconnect(Helpers.parseMessage(ConfigManager.getConfig().getBanScreenMessage(), Helpers.getTemplateFor(punishment)));
			}
		}
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
}
