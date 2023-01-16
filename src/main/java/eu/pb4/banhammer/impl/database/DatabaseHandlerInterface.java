package eu.pb4.banhammer.impl.database;

import eu.pb4.banhammer.api.PunishmentData;
import eu.pb4.banhammer.api.PunishmentType;

import java.util.function.Consumer;

public interface DatabaseHandlerInterface {
    boolean insertPunishment(PunishmentData punishment);
    void getPunishments(String id, PunishmentType type, Consumer<PunishmentData.Synced> consumer);
    void getAllPunishments(PunishmentType type, Consumer<PunishmentData.Synced> consumer);
    void getPunishmentsHistory(String toString, Consumer<PunishmentData> consumer);
    void getAllPunishmentsHistory(Consumer<PunishmentData> consumer);
    int removePunishment(long id, PunishmentType type);
    int removePunishment(String id, PunishmentType type);

    void closeConnection();

    boolean insertPunishmentIntoHistory(PunishmentData punishment);

    String name();
}
