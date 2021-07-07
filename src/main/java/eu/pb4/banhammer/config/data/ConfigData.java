package eu.pb4.banhammer.config.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ConfigData {
    public int CONFIG_VERSION_DONT_TOUCH_THIS = 1;

    public boolean punishmentsAreSilent = false;
    public boolean storeAllPunishmentsInHistory = true;
    public List<String> muteBlockedCommands = Arrays.asList("msg", "me");
    public String defaultTempPunishmentDurationLimit = "-1";
    public HashMap<String, String> permissionTempLimit = exampleTempLimit();

    public boolean standardBanPlayersWithBannedIps = false;
    public boolean autoBansFromIpBansAreSilent = true;

    public int defaultOpPermissionLevel = 3;

    public String discordWebhookUrl = "";

    public String databaseType = "sqlite";

    public String sqliteDatabaseLocation = "banhammer-sqlite.db";
    public String mysqlDatabaseAddress = "";
    public String mysqlDatabaseName = "";
    public String mysqlDatabaseUsername = "";
    public String mysqlDatabasePassword = "";


    static private HashMap<String, String> exampleTempLimit() {
        HashMap<String, String> map = new HashMap<>();
        map.put("example", "31d");
        return map;
    }
}
