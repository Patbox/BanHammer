package eu.pb4.banhammer.types;

import eu.pb4.banhammer.Helpers;
import eu.pb4.banhammer.config.ConfigManager;
import eu.pb4.banhammer.config.MessageConfigData;
import eu.pb4.placeholders.PlaceholderAPI;
import net.minecraft.text.LiteralText;
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
        return this.isTemporary() && this.time + this.duration < System.currentTimeMillis() / 1000;
    }

    public Date getExpirationDate() {
        return this.isTemporary() ? new Date((this.time + this.duration) * 1000) : new Date(Long.MAX_VALUE - 1);
    }

    public String getFormattedExpirationDate() {
        return this.isTemporary() ? ConfigManager.getConfig().dateTimeFormatter.format(this.getExpirationDate()) : ConfigManager.getConfig().neverExpires;
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

            StringBuilder builder = new StringBuilder();

            if (years > 0) {
                builder.append(years + data.yearsText);
            }
            if (days > 0) {
                builder.append(days + data.daysText);
            }
            if (hours > 0) {
                builder.append(hours + data.hoursText);
            }
            if (minutes > 0) {
                builder.append(minutes + data.minutesText);
            }
            if (seconds > 0) {
                builder.append(seconds + data.secondsText);
            }
            return builder.toString();
        } else {
            return ConfigManager.getConfig().neverExpires;
        }
    }


    public Text getDisconnectMessage() {
        Text message;

        switch (this.type) {
            case KICK:
                message = ConfigManager.getConfig().kickScreenMessage;
                break;
            case BAN:
                message = this.isTemporary() ? ConfigManager.getConfig().tempBanScreenMessage : ConfigManager.getConfig().banScreenMessage;
                break;
            case IPBAN:
                message = this.isTemporary() ? ConfigManager.getConfig().tempIpBanScreenMessage : ConfigManager.getConfig().ipBanScreenMessage;
                break;
            default:
                message = LiteralText.EMPTY;
        }

        return PlaceholderAPI.parsePredefinedText(message, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, Helpers.getTemplateFor(this));
    }

    public Text getChatMessage() {
        Text message;

        switch (this.type) {
            case KICK:
                message = ConfigManager.getConfig().kickChatMessage;
                break;
            case BAN:
                message = this.isTemporary() ? ConfigManager.getConfig().tempBanChatMessage : ConfigManager.getConfig().banChatMessage;
                break;
            case IPBAN:
                message = this.isTemporary() ? ConfigManager.getConfig().tempIpBanChatMessage : ConfigManager.getConfig().ipBanChatMessage;
                break;
            case MUTE:
                message = this.isTemporary() ? ConfigManager.getConfig().tempMuteChatMessage : ConfigManager.getConfig().muteChatMessage;
                break;
            default:
                message = LiteralText.EMPTY;
        }

        return PlaceholderAPI.parsePredefinedText(message, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, Helpers.getTemplateFor(this));
    }

    public boolean isTemporary() {
        return this.duration > -1;
    }
}
