package eu.pb4.banhammer.impl.database;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class PooledSQLDatabase extends AbstractSQLDatabase {
    protected MiniConnectionPoolManager manager;


    @Override
    protected Connection getConnection() throws SQLException {
        return this.manager.getConnection();
    }

    @Override
    public void closeConnection() {
        try {
            this.manager.dispose();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
