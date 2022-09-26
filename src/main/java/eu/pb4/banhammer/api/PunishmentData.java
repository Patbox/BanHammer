package eu.pb4.banhammer.api;

import eu.pb4.banhammer.impl.BHUtils;
import eu.pb4.banhammer.impl.config.Config;
import eu.pb4.banhammer.impl.config.ConfigManager;
import eu.pb4.banhammer.impl.config.data.DiscordMessageData;
import eu.pb4.banhammer.impl.config.data.MessageConfigData;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.node.EmptyNode;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.ApiStatus;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public sealed class PunishmentData permits PunishmentData.Synced {
    public final PunishmentType type;
    public final long time;
    public final long duration;
    public final UUID playerUUID;
    public final String playerIP;
    public final UUID adminUUID;
    public final Text adminDisplayName;
    public final String reason;
    public final Text playerDisplayName;
    public final String playerName;

    @ApiStatus.Internal
    public PunishmentData(UUID playerUUID, String playerIP, Text playerName, String playerNameRaw, UUID adminUUID, Text adminDisplay, long time, long duration, String reason, PunishmentType type) {
        this.time = time;
        this.playerUUID = playerUUID;
        this.playerIP = playerIP;
        this.playerName = playerNameRaw;
        this.playerDisplayName = playerName;
        this.duration = duration;
        this.adminDisplayName = adminDisplay;
        this.adminUUID = adminUUID;
        this.reason = reason;
        this.type = type;
    }

    public static PunishmentData create(ServerPlayerEntity punished, ServerCommandSource admin, String reason, long duration, PunishmentType type) {
        return create(punished.getUuid(), punished.getIp(), punished.getDisplayName(), punished.getGameProfile().getName(), admin, reason, duration, type);
    }

    public static PunishmentData create(UUID uuid, String ip, Text displayName, String playerName, ServerCommandSource admin, String reason, long duration, PunishmentType type) {
        return new PunishmentData(uuid, ip, displayName, playerName, admin.getEntity() != null ? admin.getEntity().getUuid() : Util.NIL_UUID, admin.getDisplayName(), BHUtils.getNow(), duration, reason, type);
    }

    public final boolean isExpired() {
        return this.isTemporary() && this.time + this.duration < System.currentTimeMillis() / 1000;
    }

    public final Date getExpirationDate() {
        return this.isTemporary() ? new Date((this.time + this.duration) * 1000) : new Date(Long.MAX_VALUE - 1);
    }

    public final Date getDate() {
        return new Date(this.time * 1000);
    }

    public final String getFormattedDate() {
        return ConfigManager.getConfig().dateTimeFormatter.format(this.getDate());
    }

    public final String getFormattedExpirationDate() {
        return this.isTemporary() ? ConfigManager.getConfig().dateTimeFormatter.format(this.getExpirationDate()) : ConfigManager.getConfig().neverExpires;
    }

    public final String getFormattedExpirationTime() {
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


    public final Text getDisconnectMessage() {
        var message = switch (this.type) {
            case KICK -> ConfigManager.getConfig().kickScreenMessage;
            case BAN -> this.isTemporary() ? ConfigManager.getConfig().tempBanScreenMessage : ConfigManager.getConfig().banScreenMessage;
            case IP_BAN -> this.isTemporary() ? ConfigManager.getConfig().tempIpBanScreenMessage : ConfigManager.getConfig().ipBanScreenMessage;
            case MUTE -> this.isTemporary() ? ConfigManager.getConfig().tempMutedMessage : ConfigManager.getConfig().mutedMessage;
            default -> EmptyNode.INSTANCE;
        };

        return Placeholders.parseText(message, Placeholders.PREDEFINED_PLACEHOLDER_PATTERN, this.getPlaceholders());
    }

    public final Text getChatMessage() {
        var message = switch (this.type) {
            case KICK -> ConfigManager.getConfig().kickChatMessage;
            case BAN -> this.isTemporary() ? ConfigManager.getConfig().tempBanChatMessage : ConfigManager.getConfig().banChatMessage;
            case IP_BAN -> this.isTemporary() ? ConfigManager.getConfig().tempIpBanChatMessage : ConfigManager.getConfig().ipBanChatMessage;
            case MUTE -> this.isTemporary() ? ConfigManager.getConfig().tempMuteChatMessage : ConfigManager.getConfig().muteChatMessage;
            case WARN -> this.isTemporary() ? ConfigManager.getConfig().tempWarnChatMessage : ConfigManager.getConfig().warnChatMessage;
        };

        return Placeholders.parseText(message, Placeholders.PREDEFINED_PLACEHOLDER_PATTERN, this.getPlaceholders());
    }

    public final DiscordMessageData.Message getRawDiscordMessage() {
        DiscordMessageData.Message message = null;

        Config config = ConfigManager.getConfig();
        var data = config.discordMessages;

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
            case IP_BAN:
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
            case WARN:
                if (this.isTemporary() && data.sendTempWarnMessage) {
                    message = data.tempWarnMessage;
                } else if (data.sendWarnMessage) {
                    message = data.warnMessage;
                }
                break;
        }

        return message;
    }

    public final Map<String, Text> getPlaceholders() {
        HashMap<String, Text> list = new HashMap<>();

        list.put("operator", this.adminDisplayName.copy());
        list.put("operator_uuid", Text.literal(this.adminUUID.toString()));
        list.put("reason", Text.literal(this.reason));
        list.put("expiration_date", Text.literal(this.getFormattedExpirationDate()));
        list.put("expiration_time", Text.literal(this.getFormattedExpirationTime()));
        list.put("banned", this.playerDisplayName.copy());
        list.put("banned_name", Text.literal(this.playerName));
        list.put("banned_uuid", Text.literal(this.playerUUID.toString()));

        return list;
    }

    public final Map<String, String> getStringPlaceholders() {
        HashMap<String, String> list = new HashMap<>();

        list.put("operator", this.adminDisplayName.getString());
        list.put("operator_uuid", this.adminUUID.toString());
        list.put("reason", this.reason);
        list.put("expiration_date", this.getFormattedExpirationDate());
        list.put("expiration_time", this.getFormattedExpirationTime());
        list.put("banned", this.playerDisplayName.getString());
        list.put("banned_name", this.playerName);
        list.put("banned_uuid", this.playerUUID.toString());

        return list;
    }

    public final boolean isTemporary() {
        return this.duration > -1;
    }

    public static final class Synced extends PunishmentData {
        private final long id;

        @ApiStatus.Internal
        public Synced(long id, UUID playerUUID, String playerIP, Text playerName, String playerNameRaw, UUID adminUUID, Text adminDisplay, long time, long duration, String reason, PunishmentType type) {
            super(playerUUID, playerIP, playerName, playerNameRaw, adminUUID, adminDisplay, time, duration, reason, type);
            this.id = id;
        }

        public final long getId() {
            return this.id;
        }
    }
}
