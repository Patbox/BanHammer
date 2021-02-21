package eu.pb4.banhammer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.pb4.banhammer.BanHammerMod;

import java.io.*;
import java.nio.file.Paths;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Config CONFIG;
    private static boolean ENABLED = false;

    public static Config getConfig() {
        return CONFIG;
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static boolean loadConfig() {
        ENABLED = false;

        CONFIG = null;
        try {
            File configDir = Paths.get("", "config", "banhammer").toFile();

            configDir.mkdirs();

            File configFile = new File(configDir, "config.json");
            File messagesFile = new File(configDir, "messages.json");

            ConfigData configData = configFile.exists() ? GSON.fromJson(new FileReader(configFile), ConfigData.class) : new ConfigData();
            MessageConfigData messageConfigData = messagesFile.exists() ? GSON.fromJson(new FileReader(messagesFile), MessageConfigData.class) : new MessageConfigData();

            CONFIG = new Config(configData, messageConfigData);

            BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));
            writer.write(GSON.toJson(configData));
            writer.close();

            BufferedWriter writer2 = new BufferedWriter(new FileWriter(messagesFile));
            writer2.write(GSON.toJson(messageConfigData));
            writer2.close();

            ENABLED = true;
        }
        catch(IOException exception) {
            ENABLED = false;
            BanHammerMod.LOGGER.error("Something went wrong while reading config!");
            exception.printStackTrace();
        }

        return ENABLED;
    }
}
