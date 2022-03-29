package eu.pb4.banhammer.impl.database;

import java.sql.DriverManager;
import java.util.Map;

public class MySQLDatabase extends AbstractSQLDatabase {
    public MySQLDatabase(String address, String database, String username, String password, Map<String, String> args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        var argBuilder = new StringBuilder();

        var iter = args.entrySet().iterator();

        while (iter.hasNext()) {
            var arg = iter.next();
            argBuilder.append(arg.getKey()).append("=").append(arg.getValue());

            if (iter.hasNext()) {
                argBuilder.append("&");
            }
        }
        conn = DriverManager.getConnection("jdbc:mysql://" + address + "/" + database + (argBuilder.isEmpty() ? "" : "?" + argBuilder), username, password);
        stat = conn.createStatement();

        this.createTables();
    }

    @Override
    protected String getTableCreation() {
        return "CREATE TABLE IF NOT EXISTS %s (id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "bannedUUID varchar(36), bannedIP varchar(40), bannedName varchar(64), bannedDisplay varchar(512), " +
                "adminUUID varchar(36), adminDisplay TEXT, time BIGINT, duration BIGINT, reason varchar(512))";
    }

    @Override
    protected String getHistoryTableCreation(String prefix) {
        return "CREATE TABLE IF NOT EXISTS " + prefix + "history (id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "bannedUUID varchar(36), bannedIP varchar(40), bannedName varchar(64), bannedDisplay varchar(512), " +
                "adminUUID varchar(36), adminDisplay TEXT, time BIGINT, duration BIGINT, reason varchar(512), type varchar(16))";
    }
}
