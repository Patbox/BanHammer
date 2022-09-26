package eu.pb4.banhammer.impl.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.pb4.banhammer.impl.BanHammerImpl;
import eu.pb4.banhammer.impl.config.data.ConfigData;
import eu.pb4.banhammer.impl.config.data.DiscordMessageData;
import eu.pb4.banhammer.impl.config.data.MessageConfigData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Predicate;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Config CONFIG;

    public static Config getConfig() {
        return CONFIG;
    }

    public static boolean loadConfig() {
        Config oldConfig = CONFIG;
        boolean success;

        CONFIG = null;
        try {
            File configDir = Paths.get("", "config", "banhammer").toFile();

            configDir.mkdirs();

            File configFile = new File(configDir, "config.json");
            File messagesFile = new File(configDir, "messages.json");
            File discordMessagesFile = new File(configDir, "discord-messages.json");

            ConfigData configData = configFile.exists() ? GSON.fromJson(new InputStreamReader(new FileInputStream(configFile), "UTF-8"), ConfigData.class) : new ConfigData();
            MessageConfigData messageConfigData = messagesFile.exists() ? GSON.fromJson(new InputStreamReader(new FileInputStream(messagesFile), "UTF-8"), MessageConfigData.class) : new MessageConfigData();
            DiscordMessageData discordConfigData = discordMessagesFile.exists() ? GSON.fromJson(new InputStreamReader(new FileInputStream(discordMessagesFile), "UTF-8"), DiscordMessageData.class) : new DiscordMessageData();

            configData.update();

            CONFIG = new Config(configData, messageConfigData, discordConfigData);

            {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), "UTF-8"));
                writer.write(GSON.toJson(configData));
                writer.close();
            }
            {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(messagesFile), "UTF-8"));
                writer.write(GSON.toJson(messageConfigData));
                writer.close();
            }
            {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(discordMessagesFile), "UTF-8"));
                writer.write(GSON.toJson(discordConfigData));
                writer.close();
            }
            success = true;
        }
        catch(IOException exception) {
            success = false;
            CONFIG = oldConfig;
            BanHammerImpl.LOGGER.error("Something went wrong while reading config!");
            exception.printStackTrace();
        }

        return success;
    }


    @NotNull
    public static Predicate<ServerCommandSource> requirePermissionOrOp(@NotNull String permission) {
        Objects.requireNonNull(permission, "permission");
        return (player) -> Permissions.check(player, permission, CONFIG.configData.defaultOpPermissionLevel);
    }
}
