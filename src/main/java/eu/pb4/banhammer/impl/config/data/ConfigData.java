package eu.pb4.banhammer.impl.config.data;

import java.util.*;

public final class ConfigData {
    public int CONFIG_VERSION_DONT_TOUCH_THIS = 1;

    public boolean punishmentsAreSilent = false;
    public boolean storeAllPunishmentsInHistory = true;
    public List<String> muteBlockedCommands = Arrays.asList("msg", "me");
    public String defaultTempPunishmentDurationLimit = "-1";
    public HashMap<String, String> permissionTempLimit = exampleTempLimit();

    public List<WarnAction> warnActions = Arrays.asList(WarnAction.of(5, "tempban ${uuid} ${count}h -s You have received too many warns! ${reason}"));

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
    public HashMap<String, String> mysqlDatabaseArgs = getMysqlArgs();
    public String databasePrefix = "";

    public Set<String> blockPunishments = new HashSet<>();


    static private HashMap<String, String> exampleTempLimit() {
        HashMap<String, String> map = new HashMap<>();
        map.put("example", "31d");
        return map;
    }

    public HashMap<String, String> getMysqlArgs() {
        var map = new HashMap<String, String>();
        map.put("autoReconnect", "true");
        map.put("useUnicode", "true");
        return map;
    }

    public static final class WarnAction {
        public int count = -1;
        public List<String> execute = List.of();

        public static WarnAction of(int i, String s) {
            var action = new WarnAction();
            action.count = i;
            action.execute = List.of(s);
            return action;
        }
    }
}
