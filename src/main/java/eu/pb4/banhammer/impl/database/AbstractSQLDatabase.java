package eu.pb4.banhammer.impl.database;

import com.google.common.net.InetAddresses;
import eu.pb4.banhammer.impl.config.ConfigManager;
import eu.pb4.banhammer.api.PunishmentData;
import eu.pb4.banhammer.api.PunishmentType;
import net.minecraft.text.Text;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;


public abstract class AbstractSQLDatabase implements DatabaseHandlerInterface {
    protected Connection conn;
    protected Statement stat;

    protected abstract String getTableCreation();
    protected abstract String getHistoryTableCreation(String prefix);

    public void createTables() throws SQLException  {
        String create = this.getTableCreation();
        String prefix = ConfigManager.getConfig().configData.databasePrefix;
        String createHistory = this.getHistoryTableCreation(prefix);

        for (var type : PunishmentType.values()) {
            if (type.databaseName != null) {
                stat.execute(String.format(create, prefix + type.databaseName));
            }
        }
        stat.execute(createHistory);
    }

    public boolean insertPunishmentIntoHistory(PunishmentData punishment) {
        try {
            PreparedStatement prepStmt = conn.prepareStatement(
                    "insert into " + ConfigManager.getConfig().configData.databasePrefix + "history (" +
                            "bannedUUID, bannedIP, bannedName, bannedDisplay," +
                            "adminUUID, adminDisplay, time, duration, reason, type) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
            prepStmt.setString(1, punishment.playerUUID.toString());
            prepStmt.setString(2, punishment.playerIP);
            prepStmt.setString(3, punishment.playerName);
            prepStmt.setString(4, Text.Serializer.toJson(punishment.playerDisplayName));
            prepStmt.setString(5, punishment.adminUUID.toString());
            prepStmt.setString(6, Text.Serializer.toJson(punishment.adminDisplayName));
            prepStmt.setLong(7, punishment.time);
            prepStmt.setLong(8, punishment.duration);
            prepStmt.setString(9, punishment.reason);
            prepStmt.setString(10, punishment.type.name);

            prepStmt.setQueryTimeout(10);

            prepStmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public boolean insertPunishment(PunishmentData punishment) {
        try {
            PreparedStatement prepStmt = conn.prepareStatement(
                    "insert into " + ConfigManager.getConfig().configData.databasePrefix + punishment.type.databaseName + "(" +
                            "bannedUUID, bannedIP, bannedName, bannedDisplay," +
                            "adminUUID, adminDisplay, time, duration, reason) values (?, ?, ?, ?, ?, ?, ?, ?, ?);");
            prepStmt.setString(1, punishment.playerUUID.toString());
            prepStmt.setString(2, punishment.playerIP);
            prepStmt.setString(3, punishment.playerName);
            prepStmt.setString(4, Text.Serializer.toJson(punishment.playerDisplayName));
            prepStmt.setString(5, punishment.adminUUID.toString());
            prepStmt.setString(6, Text.Serializer.toJson(punishment.adminDisplayName));
            prepStmt.setLong(7, punishment.time);
            prepStmt.setLong(8, punishment.duration);
            prepStmt.setString(9, punishment.reason);

            prepStmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void getPunishments(String id, PunishmentType type, Consumer<PunishmentData.Synced> consumer) {
        try {
            String query = "SELECT * FROM " + ConfigManager.getConfig().configData.databasePrefix + type.databaseName + " WHERE " + (InetAddresses.isInetAddress(id) ? "bannedIP" : "bannedUUID") + "='" + id + "';";
            ResultSet result = stat.executeQuery(query);

            while(!result.isClosed() && result.next()) {
                consumer.accept(new PunishmentData.Synced(
                        result.getLong("id"),
                        UUID.fromString(result.getString("bannedUUID")),
                        result.getString("bannedIP"),
                        Text.Serializer.fromJson(result.getString("bannedDisplay")),
                        result.getString("bannedName"),
                        UUID.fromString(result.getString("adminUUID")),
                        Text.Serializer.fromJson(result.getString("adminDisplay")),
                        result.getLong("time"),
                        result.getLong("duration"),
                        result.getString("reason"),
                        type
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getPunishmentsHistory(String id, Consumer<PunishmentData> consumer) {
        try {
            String query = "SELECT * FROM " + ConfigManager.getConfig().configData.databasePrefix + "history WHERE " + (InetAddresses.isInetAddress(id) ? "bannedIP" : "bannedUUID") + "='" + id + "';";
            ResultSet result = stat.executeQuery(query);

            while(!result.isClosed() && result.next()) {
                consumer.accept(new PunishmentData(
                        UUID.fromString(result.getString("bannedUUID")),
                        result.getString("bannedIP"),
                        Text.Serializer.fromJson(result.getString("bannedDisplay")),
                        result.getString("bannedName"),
                        UUID.fromString(result.getString("adminUUID")),
                        Text.Serializer.fromJson(result.getString("adminDisplay")),
                        result.getLong("time"),
                        result.getLong("duration"),
                        result.getString("reason"),
                        PunishmentType.fromName(result.getString("type"))
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getAllPunishmentsHistory(Consumer<PunishmentData> consumer) {
        try {
            String query = "SELECT * FROM " + ConfigManager.getConfig().configData.databasePrefix + "history;";
            ResultSet result = stat.executeQuery(query);

            while(!result.isClosed() && result.next()) {
                consumer.accept(new PunishmentData(
                        UUID.fromString(result.getString("bannedUUID")),
                        result.getString("bannedIP"),
                        Text.Serializer.fromJson(result.getString("bannedDisplay")),
                        result.getString("bannedName"),
                        UUID.fromString(result.getString("adminUUID")),
                        Text.Serializer.fromJson(result.getString("adminDisplay")),
                        result.getLong("time"),
                        result.getLong("duration"),
                        result.getString("reason"),
                        PunishmentType.fromName(result.getString("type"))
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getAllPunishments(PunishmentType type, Consumer<PunishmentData.Synced> consumer) {
        try {
            String query = "SELECT * FROM " + ConfigManager.getConfig().configData.databasePrefix + type.databaseName + ";";
            ResultSet result = stat.executeQuery(query);

            while(!result.isClosed() && result.next()) {
                consumer.accept(new PunishmentData.Synced(
                        result.getLong("id"),
                        UUID.fromString(result.getString("bannedUUID")),
                        result.getString("bannedIP"),
                        Text.Serializer.fromJson(result.getString("bannedDisplay")),
                        result.getString("bannedName"),
                        UUID.fromString(result.getString("adminUUID")),
                        Text.Serializer.fromJson(result.getString("adminDisplay")),
                        result.getLong("time"),
                        result.getLong("duration"),
                        result.getString("reason"),
                        type
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int removePunishment(long id, PunishmentType type) {
        try {
            return stat.executeUpdate("DELETE FROM " + ConfigManager.getConfig().configData.databasePrefix + type.databaseName + " WHERE id=" + id + ";");
        } catch (Exception x) {
            x.printStackTrace();
            return 0;
        }
    }

    @Override
    public int removePunishment(String id, PunishmentType type) {
        try {
            return stat.executeUpdate("DELETE FROM " + ConfigManager.getConfig().configData.databasePrefix + type.databaseName + " WHERE " + (InetAddresses.isInetAddress(id) ? "bannedIP" : "bannedUUID") + "='" + id + "';");
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void closeConnection() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
