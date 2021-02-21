package eu.pb4.banhammer.types;

import eu.pb4.banhammer.config.MessageConfigData;
import eu.pb4.banhammer.config.ConfigManager;
import net.minecraft.text.Text;

import java.util.Date;
import java.util.UUID;

public class BasicPunishment {
    private final PunishmentTypes type;
    private final long time;
    private final long duration;
    private final UUID bannedUUID;
    private final String bannedIP;
    private final UUID adminUUID;
    private final Text adminDisplay;
    private final String reason;
    private final Text bannedName;
    private final String bannedNameRaw;

    public BasicPunishment(UUID playerUUID, String playerIP, Text playerName, String playerNameRaw, UUID adminUUID, Text adminDisplay, long time, long duration, String reason, PunishmentTypes type) {
        this.time = time;
        this.bannedUUID = playerUUID;
        this.bannedIP = playerIP;
        this.bannedNameRaw = playerNameRaw;
        this.bannedName = playerName;
        this.duration = duration;
        this.adminDisplay = adminDisplay;
        this.adminUUID = adminUUID;
        this.reason = reason;
        this.type = type;
    }

    public PunishmentTypes getType() {
        return this.type;
    }

    public long getTime() {
        return this.time;
    }

    public long getDuration() {
        return this.duration;
    }

    public UUID getUUIDOfAdmin() {
        return this.adminUUID;
    }

    public Text getNameOfAdmin() {
        return this.adminDisplay;
    }

    public UUID getUUIDofPlayer() {
        return this.bannedUUID;
    }

    public String getIPofPlayer() {
        return this.bannedIP;
    }

    public Text getNameOfPlayer() {
        return this.bannedName;
    }

    public String getRawNameOfPlayer() {
        return this.bannedNameRaw;
    }

    public String getReason() { return this.reason; }

    public boolean isExpired() {
        return this.duration > -1 && this.time + this.duration < System.currentTimeMillis() / 1000;
    }

    public Date getExpirationDate() {
        return this.duration > -1 ? new Date((this.time + this.duration ) * 1000) : new Date(32503676400000l);
    }

    public String getFormattedExpirationDate() {
        return this.duration > -1 ? ConfigManager.getConfig().getDateFormatter().format(this.getExpirationDate()) : ConfigManager.getConfig().getNeverExpires();
    }

    public String getFormattedExpirationTime() {
        if (this.duration > -1) {
            long x = this.duration + this.time - System.currentTimeMillis() / 1000;

            MessageConfigData data = ConfigManager.getConfig().getMessageConfigData();

            long seconds = x % 60;
            long minutes = (x / 60) % 60;
            long hours = (x / (60 * 60)) % 24;
            long days = x / (60 * 60 * 24) % 365;
            long years = x / (60 * 60 * 24 * 365);

            if (years > 0) {
                return String.format("%d%s%d%s%d%s%d%s%d%s", years, data.yearsText, days, data.daysText, hours, data.hoursText, minutes, data.minutesText, seconds, data.secondsText);
            } else if (days > 0) {
                return String.format("%d%s%d%s%d%s%d%s", days, data.daysText, hours, data.hoursText, minutes, data.minutesText, seconds, data.secondsText);
            } else if (hours > 0) {
                return String.format("%d%s%d%s%d%s", hours, data.hoursText, minutes, data.minutesText, seconds, data.secondsText);
            } else if (minutes > 0) {
                return String.format("%d%s%d%s", minutes, data.minutesText, seconds, data.secondsText);
            } else if (seconds > 0) {
                return String.format("%d%s", seconds, data.secondsText);
            } else {
                return String.format("0%s", data.secondsText);
            }
        } else {
            return ConfigManager.getConfig().getNeverExpires();
        }
    }
}
