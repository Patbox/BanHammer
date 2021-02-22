package eu.pb4.banhammer.database;

import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteDatabase extends AbstractSQLDatabase {
    public SQLiteDatabase(String database) throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:" + database);

        stat = conn.createStatement();
        this.createTables();
    }
}
