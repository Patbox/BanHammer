package eu.pb4.banhammer.impl.config.data;

import java.util.*;

public final class ConfigData {
    public int CONFIG_VERSION_DONT_TOUCH_THIS = 1;

    public boolean punishmentsAreSilent = false;
    public boolean storeAllPunishmentsInHistory = true;
    public List<String> muteBlockedCommands = Arrays.asList("me", "msg", "tell", "w", "teammsg");
    public String defaultTempPunishmentDurationLimit = "-1";
    public HashMap<String, String> permissionTempLimit = exampleTempLimit();

    public List<WarnAction> warnActions = Arrays.asList(WarnAction.of(5, "tempban ${uuid} ${count}h -s You have received too many warns! ${reason}"));

    public boolean standardBanPlayersWithBannedIps = false;
    public boolean autoBansFromIpBansAreSilent = true;

    public int defaultOpPermissionLevel = 3;
    public boolean cachePunishmentsLocally = true;

    public List<String> discordWebhookUrls = new ArrayList<>();

    public String databaseType = "sqlite";
    public String sqliteDatabaseLocation = "banhammer-sqlite.db";

    public String databaseAddress = "";
    public String databaseName = "";
    public String databaseUsername = "";
    public String databasePassword = "";
    public HashMap<String, String> databaseArgs = getMysqlArgs();
    public String databasePrefix = "";

    public Set<String> blockPunishments = new HashSet<>();

    @Deprecated
    public String mysqlDatabaseAddress = null;
    @Deprecated
    public String mysqlDatabaseName = null;
    @Deprecated
    public String mysqlDatabaseUsername = null;
    @Deprecated
    public String mysqlDatabasePassword = null;
    @Deprecated
    public HashMap<String, String> mysqlDatabaseArgs = null;

    public void update() {
        if (mysqlDatabaseAddress != null) {
            databaseAddress = mysqlDatabaseAddress;
            mysqlDatabaseAddress = null;
        }

        if (mysqlDatabaseName != null) {
            databaseName = mysqlDatabaseName;
            mysqlDatabaseName = null;
        }

        if (mysqlDatabaseUsername != null) {
            databaseUsername = mysqlDatabaseUsername;
            mysqlDatabaseUsername = null;
        }

        if (mysqlDatabasePassword != null) {
            databasePassword = mysqlDatabasePassword;
            mysqlDatabasePassword = null;
        }

        if (mysqlDatabaseArgs != null) {
            databaseArgs = mysqlDatabaseArgs;
            mysqlDatabaseArgs = null;
        }
    }

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
