package eu.pb4.banhammer.database;

import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLDatabase extends AbstractSQLDatabase {
    public MySQLDatabase(String address, String database, String username, String password) throws SQLException {
        conn = DriverManager.getConnection("jdbc:mysql://" + address + "/" + database, username, password);
        stat = conn.createStatement();

        this.createTables();
    }
}
