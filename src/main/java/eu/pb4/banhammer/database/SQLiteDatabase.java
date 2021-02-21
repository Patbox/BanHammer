package eu.pb4.banhammer.database;

import eu.pb4.banhammer.types.BasicPunishment;
import eu.pb4.banhammer.types.PunishmentTypes;
import eu.pb4.banhammer.types.SyncedPunishment;
import net.minecraft.text.Text;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class SQLiteDatabase implements DatabaseHandlerInterface {
    public static final String DB_URL = "jdbc:sqlite:banhammer.db";

    private Connection conn;
    private Statement stat;

    public SQLiteDatabase() {
        try {
            conn = DriverManager.getConnection(DB_URL);
            stat = conn.createStatement();
        } catch (SQLException e) {
            System.err.println("Couldn't create SQLite database!");
            e.printStackTrace();
        }

        this.createTables();
    }

    public boolean createTables()  {
        String create = "CREATE TABLE IF NOT EXISTS %s (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "bannedUUID varchar(36), bannedIP varchar(15), bannedName varchar(16), bannedDisplay varchar(512), " +
                "adminUUID varchar(36), adminDisplay varchar(512), time BIGINT, duration BIGINT, reason varchar(128))";
        String createHistory = "CREATE TABLE IF NOT EXISTS history (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "bannedUUID varchar(36), bannedIP varchar(15), bannedName varchar(16), bannedDisplay varchar(512), " +
                "adminUUID varchar(36), adminDisplay varchar(512), time BIGINT, duration BIGINT, reason varchar(128), type varchar(16))";

        try {
            stat.execute(String.format(create, PunishmentTypes.BAN.databaseName));
            stat.execute(String.format(create, PunishmentTypes.IPBAN.databaseName));
            stat.execute(String.format(create, PunishmentTypes.MUTE.databaseName));
            stat.execute(createHistory);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean insertPunishmentIntoHistory(BasicPunishment punishment) {
        try {
            PreparedStatement prepStmt = conn.prepareStatement(
                    "insert into history values (NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
            prepStmt.setString(1, punishment.getUUIDofPlayer().toString());
            prepStmt.setString(2, punishment.getIPofPlayer());
            prepStmt.setString(3, punishment.getRawNameOfPlayer());
            prepStmt.setString(4, Text.Serializer.toJson(punishment.getNameOfPlayer()));
            prepStmt.setString(5, punishment.getUUIDOfAdmin().toString());
            prepStmt.setString(6, Text.Serializer.toJson(punishment.getNameOfAdmin()));
            prepStmt.setString(7, String.valueOf(punishment.getTime()));
            prepStmt.setString(8, String.valueOf(punishment.getDuration()));
            prepStmt.setString(9, punishment.getReason());
            prepStmt.setString(10, punishment.getType().name);

            prepStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public boolean insertPunishment(BasicPunishment punishment) {
        try {
            PreparedStatement prepStmt = conn.prepareStatement(
                    "insert into " + punishment.getType().databaseName + " values (NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
            prepStmt.setString(1, punishment.getUUIDofPlayer().toString());
            prepStmt.setString(2, punishment.getIPofPlayer());
            prepStmt.setString(3, punishment.getRawNameOfPlayer());
            prepStmt.setString(4, Text.Serializer.toJson(punishment.getNameOfPlayer()));
            prepStmt.setString(5, punishment.getUUIDOfAdmin().toString());
            prepStmt.setString(6, Text.Serializer.toJson(punishment.getNameOfAdmin()));
            prepStmt.setString(7, String.valueOf(punishment.getTime()));
            prepStmt.setString(8, String.valueOf(punishment.getDuration()));
            prepStmt.setString(9, punishment.getReason());

            prepStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public List<SyncedPunishment> getPunishments(String id, PunishmentTypes type) {
        List<SyncedPunishment> list = new LinkedList<>();
        try {
            String query = "SELECT * FROM " + type.databaseName + " WHERE " + (type.ipBased ? "bannedIP" : "bannedUUID") + "='" + id + "';";
            ResultSet result = stat.executeQuery(query);
            UUID bannedUUID;
            String bannedIP;
            String bannedNameRaw;
            Text bannedName;
            UUID adminUUID;
            Text adminName;
            long time;
            long duration;
            String reason;


            while(result.next()) {
                long idd = result.getLong("id");
                bannedUUID = UUID.fromString(result.getString("bannedUUID"));
                bannedIP = result.getString("bannedIP");
                bannedNameRaw = result.getString("bannedName");
                bannedName = Text.Serializer.fromJson(result.getString("bannedDisplay"));
                adminUUID = UUID.fromString(result.getString("adminUUID"));
                adminName = Text.Serializer.fromJson(result.getString("adminDisplay"));
                time = result.getLong("time");
                duration = result.getLong("duration");
                reason = result.getString("reason");

                list.add(new SyncedPunishment(idd, bannedUUID, bannedIP, bannedName, bannedNameRaw, adminUUID, adminName, time, duration, reason, type));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return list;
    }

    @Override
    public List<SyncedPunishment> getAllPunishments(PunishmentTypes type) {
        List<SyncedPunishment> list = new LinkedList<>();
        try {
            String query = "SELECT * FROM " + type.databaseName + ";";
            ResultSet result = stat.executeQuery(query);
            UUID bannedUUID;
            String bannedIP;
            String bannedNameRaw;
            Text bannedName;
            UUID adminUUID;
            Text adminName;
            long time;
            long duration;
            String reason;


            while(result.next()) {
                long idd = result.getLong("id");
                bannedUUID = UUID.fromString(result.getString("bannedUUID"));
                bannedIP = result.getString("bannedIP");
                bannedNameRaw = result.getString("bannedName");
                bannedName = Text.Serializer.fromJson(result.getString("bannedDisplay"));
                adminUUID = UUID.fromString(result.getString("adminUUID"));
                adminName = Text.Serializer.fromJson(result.getString("adminDisplay"));
                time = result.getLong("time");
                duration = result.getLong("duration");
                reason = result.getString("reason");

                list.add(new SyncedPunishment(idd, bannedUUID, bannedIP, bannedName, bannedNameRaw, adminUUID, adminName, time, duration, reason, type));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return list;
    }

    @Override
    public void removePunishment(long id, PunishmentTypes type) {
        try {
            stat.execute("DELETE FROM " + type.databaseName + " WHERE id=" + id + ";");
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    @Override
    public void removePunishment(String id, PunishmentTypes type) {
        try {
            stat.execute("DELETE FROM " + type.databaseName + " WHERE " + (type.ipBased ? "bannedIP" : "bannedUUID") + "='" + id + "';");
        } catch (Exception x) {
            x.printStackTrace();
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
