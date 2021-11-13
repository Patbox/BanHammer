package eu.pb4.banhammer.types;

import eu.pb4.banhammer.config.Config;
import eu.pb4.banhammer.config.ConfigManager;
import eu.pb4.banhammer.config.data.DiscordMessageData;
import eu.pb4.banhammer.config.data.MessageConfigData;
import eu.pb4.placeholders.PlaceholderAPI;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
                builder.append(years).append(data.yearsText);
            }
            if (days > 0) {
                builder.append(days).append(data.daysText);
            }
            if (hours > 0) {
                builder.append(hours).append(data.hoursText);
            }
            if (minutes > 0) {
                builder.append(minutes).append(data.minutesText);
            }
            if (seconds > 0) {
                builder.append(seconds).append(data.secondsText);
            }
            return builder.toString();
        } else {
            return ConfigManager.getConfig().neverExpires;
        }
    }


    public Text getDisconnectMessage() {
        Text message = switch (this.type) {
            case KICK -> ConfigManager.getConfig().kickScreenMessage;
            case BAN -> this.isTemporary() ? ConfigManager.getConfig().tempBanScreenMessage : ConfigManager.getConfig().banScreenMessage;
            case IPBAN -> this.isTemporary() ? ConfigManager.getConfig().tempIpBanScreenMessage : ConfigManager.getConfig().ipBanScreenMessage;
            case MUTE -> this.isTemporary() ? ConfigManager.getConfig().tempMutedMessage : ConfigManager.getConfig().mutedMessage;
            default -> LiteralText.EMPTY;
        };

        return PlaceholderAPI.parsePredefinedText(message, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, this.getPlaceholders());
    }

    public Text getChatMessage() {
        Text message = switch (this.type) {
            case KICK -> ConfigManager.getConfig().kickChatMessage;
            case BAN -> this.isTemporary() ? ConfigManager.getConfig().tempBanChatMessage : ConfigManager.getConfig().banChatMessage;
            case IPBAN -> this.isTemporary() ? ConfigManager.getConfig().tempIpBanChatMessage : ConfigManager.getConfig().ipBanChatMessage;
            case MUTE -> this.isTemporary() ? ConfigManager.getConfig().tempMuteChatMessage : ConfigManager.getConfig().muteChatMessage;
        };

        return PlaceholderAPI.parsePredefinedText(message, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, this.getPlaceholders());
    }

    public DiscordMessageData.Message getRawDiscordMessage() {
        DiscordMessageData.Message message = null;

        Config config = ConfigManager.getConfig();
        DiscordMessageData data = config.discordMessages;

        switch (this.type) {
            case KICK:
                if (data.sendKickMessage) {
                    message = data.kickMessage;
                }
                break;
            case BAN:
                if (this.isTemporary() && data.sendTempBanMessage) {
                    message = data.tempBanMessage;
                } else if (data.sendBanMessage) {
                    message = data.banMessage;
                }
                break;
            case IPBAN:
                if (this.isTemporary() && data.sendTempBanIpMessage) {
                    message = data.tempBanIpMessage;
                } else if (data.sendBanIpMessage) {
                    message = data.banIpMessage;
                }
                break;
            case MUTE:
                if (this.isTemporary() && data.sendTempMuteMessage) {
                    message = data.tempMuteMessage;
                } else if (data.sendMuteMessage) {
                    message = data.muteMessage;
                }
                break;
        }

        return message;
    }

    public Map<String, Text> getPlaceholders() {
        HashMap<String, Text> list = new HashMap<>();

        list.put("operator", this.adminDisplayName.shallowCopy());
        list.put("operator_uuid", new LiteralText(this.adminUUID.toString()));
        list.put("reason", new LiteralText(this.reason));
        list.put("expiration_date", new LiteralText(this.getFormattedExpirationDate()));
        list.put("expiration_time", new LiteralText(this.getFormattedExpirationTime()));
        list.put("banned", this.bannedDisplayName.shallowCopy());
        list.put("banned_uuid", new LiteralText(this.bannedUUID.toString()));

        return list;
    }

    public Map<String, String> getStringPlaceholders() {
        HashMap<String, String> list = new HashMap<>();

        list.put("operator", this.adminDisplayName.getString());
        list.put("operator_uuid", this.adminUUID.toString());
        list.put("reason", this.reason);
        list.put("expiration_date", this.getFormattedExpirationDate());
        list.put("expiration_time", this.getFormattedExpirationTime());
        list.put("banned", this.bannedDisplayName.getString());
        list.put("banned_uuid", this.bannedUUID.toString());

        return list;
    }

    public boolean isTemporary() {
        return this.duration > -1;
    }
}
