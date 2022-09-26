package eu.pb4.banhammer.impl;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;

public class CardboardWarning implements PreLaunchEntrypoint {
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onPreLaunch() {
        checkAndAnnounce();
    }

    public static void checkAndAnnounce() {
        if (FabricLoader.getInstance().isModLoaded("cardboard")) {
            LOGGER.error("==============================================");
            LOGGER.error("");
            LOGGER.error("Cardboard detected! This mod doesn't work with it!");
            LOGGER.error("You won't get any support as long as it's present!");
            LOGGER.error("");
            LOGGER.error("Read more at: https://gist.github.com/Patbox/e44844294c358b614d347d369b0fc3bf");
            LOGGER.error("");
            LOGGER.error("==============================================");
        }
    }
}
