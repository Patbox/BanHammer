package eu.pb4.banhammer.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ConfigData {
    public int CONFIG_VERSION_DONT_TOUCH_THIS = 1;

    public boolean punishmentsAreSilent = false;
    public boolean storeAllPunishmentsInHistory = true;
    public List<String> muteBlockedCommands = Arrays.asList("msg", "me");
    public String defaultTempPunishmentDurationLimit = "-1";
    public HashMap<String, String> permissionTempLimit = new HashMap<>();
}
