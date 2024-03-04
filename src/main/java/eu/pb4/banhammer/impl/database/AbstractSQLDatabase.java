package eu.pb4.banhammer.impl.database;

import com.google.common.net.InetAddresses;
import eu.pb4.banhammer.impl.config.ConfigManager;
import eu.pb4.banhammer.api.PunishmentData;
import eu.pb4.banhammer.api.PunishmentType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;

import java.sql.*;
import java.util.UUID;
import java.util.function.Consumer;


public abstract class AbstractSQLDatabase implements DatabaseHandlerInterface {
    protected abstract String getTableCreation();

    protected abstract String getHistoryTableCreation(String prefix);

    public void createTables() throws SQLException {
        try (var conn = this.getConnection();
             var stat = conn.createStatement()) {

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

    }

    public boolean insertPunishmentIntoHistory(PunishmentData punishment) {
        try (var conn = this.getConnection();
             var prepStmt = conn.prepareStatement(
                     "insert into " + ConfigManager.getConfig().configData.databasePrefix + "history (" +
                             "bannedUUID, bannedIP, bannedName, bannedDisplay," +
                             "adminUUID, adminDisplay, time, duration, reason, type) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {

            prepStmt.setString(1, punishment.playerUUID.toString());
            prepStmt.setString(2, punishment.playerIP);
            prepStmt.setString(3, punishment.playerName);
            prepStmt.setString(4, Text.Serialization.toJsonString(punishment.playerDisplayName, DynamicRegistryManager.EMPTY));
            prepStmt.setString(5, punishment.adminUUID.toString());
            prepStmt.setString(6, Text.Serialization.toJsonString(punishment.adminDisplayName, DynamicRegistryManager.EMPTY));
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
        try (var conn = this.getConnection();
            var prepStmt = conn.prepareStatement(
                    "insert into " + ConfigManager.getConfig().configData.databasePrefix + punishment.type.databaseName + "(" +
                            "bannedUUID, bannedIP, bannedName, bannedDisplay," +
                            "adminUUID, adminDisplay, time, duration, reason) values (?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
            prepStmt.setString(1, punishment.playerUUID.toString());
            prepStmt.setString(2, punishment.playerIP);
            prepStmt.setString(3, punishment.playerName);
            prepStmt.setString(4, Text.Serialization.toJsonString(punishment.playerDisplayName, DynamicRegistryManager.EMPTY));
            prepStmt.setString(5, punishment.adminUUID.toString());
            prepStmt.setString(6, Text.Serialization.toJsonString(punishment.adminDisplayName, DynamicRegistryManager.EMPTY));
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
        try (var conn = this.getConnection();
             var stat = conn.prepareStatement("SELECT * FROM " + ConfigManager.getConfig().configData.databasePrefix + type.databaseName + " WHERE " + (InetAddresses.isInetAddress(id) ? "bannedIP" : "bannedUUID") + "='" + id + "';");
             var result = stat.executeQuery()
        ) {
            while (!result.isClosed() && result.next()) {
                consumer.accept(new PunishmentData.Synced(
                        result.getLong("id"),
                        UUID.fromString(result.getString("bannedUUID")),
                        result.getString("bannedIP"),
                        Text.Serialization.fromJson(result.getString("bannedDisplay"), DynamicRegistryManager.EMPTY),
                        result.getString("bannedName"),
                        UUID.fromString(result.getString("adminUUID")),
                        Text.Serialization.fromJson(result.getString("adminDisplay"), DynamicRegistryManager.EMPTY),
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
        try (var conn = this.getConnection();
             var stat = conn.prepareStatement("SELECT * FROM " + ConfigManager.getConfig().configData.databasePrefix + "history WHERE " + (InetAddresses.isInetAddress(id) ? "bannedIP" : "bannedUUID") + "='" + id + "';");
             var result = stat.executeQuery()
        ) {
            while (!result.isClosed() && result.next()) {
                consumer.accept(new PunishmentData(
                        UUID.fromString(result.getString("bannedUUID")),
                        result.getString("bannedIP"),
                        Text.Serialization.fromJson(result.getString("bannedDisplay"), DynamicRegistryManager.EMPTY),
                        result.getString("bannedName"),
                        UUID.fromString(result.getString("adminUUID")),
                        Text.Serialization.fromJson(result.getString("adminDisplay"), DynamicRegistryManager.EMPTY),
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
        try (var conn = this.getConnection();
             var stat = conn.prepareStatement("SELECT * FROM " + ConfigManager.getConfig().configData.databasePrefix + "history;");
             var result = stat.executeQuery()
        ) {

            while (!result.isClosed() && result.next()) {
                consumer.accept(new PunishmentData(
                        UUID.fromString(result.getString("bannedUUID")),
                        result.getString("bannedIP"),
                        Text.Serialization.fromJson(result.getString("bannedDisplay"), DynamicRegistryManager.EMPTY),
                        result.getString("bannedName"),
                        UUID.fromString(result.getString("adminUUID")),
                        Text.Serialization.fromJson(result.getString("adminDisplay"), DynamicRegistryManager.EMPTY),
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
        try (var conn = this.getConnection();
             var stat = conn.prepareStatement("SELECT * FROM " + ConfigManager.getConfig().configData.databasePrefix + type.databaseName + ";");
             var result = stat.executeQuery()
        ) {
            while (!result.isClosed() && result.next()) {
                consumer.accept(new PunishmentData.Synced(
                        result.getLong("id"),
                        UUID.fromString(result.getString("bannedUUID")),
                        result.getString("bannedIP"),
                        Text.Serialization.fromJson(result.getString("bannedDisplay"), DynamicRegistryManager.EMPTY),
                        result.getString("bannedName"),
                        UUID.fromString(result.getString("adminUUID")),
                        Text.Serialization.fromJson(result.getString("adminDisplay"), DynamicRegistryManager.EMPTY),
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

    protected abstract Connection getConnection() throws SQLException;

    @Override
    public int removePunishment(long id, PunishmentType type) {
        try (var conn = this.getConnection();
             var stat = conn.prepareStatement("DELETE FROM " + ConfigManager.getConfig().configData.databasePrefix + type.databaseName + " WHERE id=" + id + ";");
        ) {
            return stat.executeUpdate();
        } catch (Exception x) {
            x.printStackTrace();
            return 0;
        }
    }

    @Override
    public int removePunishment(String id, PunishmentType type) {
        try (var conn = this.getConnection();
             var stat = conn.prepareStatement("DELETE FROM " + ConfigManager.getConfig().configData.databasePrefix + type.databaseName + " WHERE " + (InetAddresses.isInetAddress(id) ? "bannedIP" : "bannedUUID") + "='" + id + "';");
        ) {
            return stat.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

}
