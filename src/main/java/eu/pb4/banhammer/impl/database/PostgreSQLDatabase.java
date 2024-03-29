package eu.pb4.banhammer.impl.database;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import org.postgresql.ds.PGPoolingDataSource;
import org.postgresql.jdbc2.optional.PoolingDataSource;

import java.sql.DriverManager;
import java.util.Map;

public class PostgreSQLDatabase extends PooledSQLDatabase {
    public PostgreSQLDatabase(String address, String database, String username, String password, Map<String, String> args) throws Exception {
        Class.forName("org.postgresql.Driver");

        var argBuilder = new StringBuilder();

        var iter = args.entrySet().iterator();

        while (iter.hasNext()) {
            var arg = iter.next();
            argBuilder.append(arg.getKey()).append("=").append(arg.getValue());

            if (iter.hasNext()) {
                argBuilder.append("&");
            }
        }

        var source = new PGPoolingDataSource();
        source.setUrl("jdbc:postgresql://" + address + "/" + database + (argBuilder.isEmpty() ? "" : "?" + argBuilder));
        source.setUser(username);
        source.setPassword(password);
        source.setDatabaseName(database);

        this.createTables();
    }

    @Override
    protected String getTableCreation() {
        return "CREATE TABLE IF NOT EXISTS %s (id SERIAL PRIMARY KEY, " +
                "bannedUUID varchar(36), bannedIP varchar(40), bannedName varchar(64), bannedDisplay TEXT, " +
                "adminUUID varchar(36), adminDisplay TEXT, time BIGINT, duration BIGINT, reason TEXT)";
    }

    @Override
    protected String getHistoryTableCreation(String prefix) {
        return "CREATE TABLE IF NOT EXISTS " + prefix + "history (id SERIAL PRIMARY KEY, " +
                "bannedUUID varchar(36), bannedIP varchar(40), bannedName varchar(64), bannedDisplay TEXT, " +
                "adminUUID varchar(36), adminDisplay TEXT, time BIGINT, duration BIGINT, reason TEXT, type varchar(16))";
    }

    @Override
    public String name() {
        return "PostgreSQL";
    }
}
