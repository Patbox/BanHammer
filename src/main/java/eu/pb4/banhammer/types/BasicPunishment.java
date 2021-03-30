package eu.pb4.banhammer.types;

import eu.pb4.banhammer.Helpers;
import eu.pb4.banhammer.config.MessageConfigData;
import eu.pb4.banhammer.config.ConfigManager;
import net.minecraft.text.Text;

import java.util.Date;
import java.util.UUID;

public class BasicPunishment {
    public final PunishmentTypes type;
    public final long time;
    public final long duration;
    public final UUID bannedUUID;
    public final String bannedIP;
    public final UUID adminUUID;
    public final Text adminDisplayName;
    public final String reason;
    public final Text bannedDisplayName;
    public final String bannedName;

    public BasicPunishment(UUID playerUUID, String playerIP, Text playerName, String playerNameRaw, UUID adminUUID, Text adminDisplay, long time, long duration, String reason, PunishmentTypes type) {
        this.time = time;
        this.bannedUUID = playerUUID;
        this.bannedIP = playerIP;
        this.bannedName = playerNameRaw;
        this.bannedDisplayName = playerName;
        this.duration = duration;
        this.adminDisplayName = adminDisplay;
        this.adminUUID = adminUUID;
        this.reason = reason;
        this.type = type;
    }

    public boolean isExpired() {
        return this.duration > -1 && this.time + this.duration < System.currentTimeMillis() / 1000;
    }

    public Date getExpirationDate() {
        return this.duration > -1 ? new Date((this.time + this.duration ) * 1000) : new Date(32503676400000L);
    }

    public String getFormattedExpirationDate() {
        return this.duration > -1 ? ConfigManager.getConfig().dateTimeFormatter.format(this.getExpirationDate()) : ConfigManager.getConfig().neverExpires;
    }

    public String getFormattedExpirationTime() {
        if (this.duration > -1) {
            long x = this.duration + this.time - System.currentTimeMillis() / 1000;

            MessageConfigData data = ConfigManager.getConfig().messageConfigData;

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
            return ConfigManager.getConfig().neverExpires;
        }
    }


    public Text getDisconnectMessage() {
        String message;

        switch (this.type) {
            case KICK:
                message = ConfigManager.getConfig().kickScreenMessage;
                break;
            case BAN:
                message = ConfigManager.getConfig().banScreenMessage;
                break;
            case IPBAN:
                message = ConfigManager.getConfig().ipBanScreenMessage;
                break;
            default:
                message = "";
        }

        return Helpers.parseMessage(message, Helpers.getTemplateFor(this));
    }

    public Text getChatMessage() {
        String message;

        switch (this.type) {
            case KICK:
                message = ConfigManager.getConfig().kickChatMessage;
                break;
            case BAN:
                message = ConfigManager.getConfig().banChatMessage;
                break;
            case IPBAN:
                message = ConfigManager.getConfig().ipBanChatMessage;
                break;
            case MUTE:
                message = ConfigManager.getConfig().muteChatMessage;
                break;
            default:
                message = "";
        }

        return Helpers.parseMessage(message, Helpers.getTemplateFor(this));
    }
}
