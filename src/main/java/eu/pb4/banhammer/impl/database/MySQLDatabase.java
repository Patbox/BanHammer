package eu.pb4.banhammer.impl.database;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import eu.pb4.banhammer.impl.config.ConfigManager;

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
        var source = new MysqlConnectionPoolDataSource();
        source.setUrl("jdbc:mysql://" + address + "/" + database + (argBuilder.isEmpty() ? "" : "?" + argBuilder));
        source.setUser(username);
        source.setPassword(password);
        source.setDatabaseName(database);

        this.manager = new MiniConnectionPoolManager(source, ConfigManager.getConfig().configData.databaseMaxConnections);
        this.createTables();
    }

    @Override
    protected String getTableCreation() {
        return "CREATE TABLE IF NOT EXISTS %s (id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "bannedUUID varchar(36), bannedIP varchar(40), bannedName varchar(64), bannedDisplay TEXT, " +
                "adminUUID varchar(36), adminDisplay TEXT, time BIGINT, duration BIGINT, reason TEXT)";
    }

    @Override
    protected String getHistoryTableCreation(String prefix) {
        return "CREATE TABLE IF NOT EXISTS " + prefix + "history (id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "bannedUUID varchar(36), bannedIP varchar(40), bannedName varchar(64), bannedDisplay TEXT, " +
                "adminUUID varchar(36), adminDisplay TEXT, time BIGINT, duration BIGINT, reason TEXT, type varchar(16))";
    }

    @Override
    public String name() {
        return "MySQL";
    }
}
