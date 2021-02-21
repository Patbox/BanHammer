package eu.pb4.banhammer.types;

import net.minecraft.text.Text;

import java.util.UUID;

public class SyncedPunishment extends BasicPunishment {
    private long id;
    public SyncedPunishment(long id, UUID playerUUID, String playerIP, Text playerName, String playerNameRaw, UUID adminUUID, Text adminDisplay, long time, long duration, String reason, PunishmentTypes type) {
        super(playerUUID, playerIP, playerName, playerNameRaw, adminUUID, adminDisplay, time, duration, reason, type);
        this.id = id;
    }

    public long getId() {
        return this.id;
    }
}
