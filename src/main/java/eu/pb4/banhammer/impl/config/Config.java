package eu.pb4.banhammer.impl.config;


import com.mojang.authlib.GameProfile;
import eu.pb4.banhammer.impl.BanHammerImpl;
import eu.pb4.banhammer.impl.BHUtils;
import eu.pb4.banhammer.impl.config.data.ConfigData;
import eu.pb4.banhammer.impl.config.data.DiscordMessageData;
import eu.pb4.banhammer.impl.config.data.MessageConfigData;
import eu.pb4.banhammer.impl.config.database.DbConfig;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.placeholders.api.node.TextNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

public class Config {
    public final List<String> mutedCommands;
    public final MessageConfigData messageConfigData;

    public final TextNode mutedMessage;
    public final TextNode tempMutedMessage;

    public final SimpleDateFormat dateTimeFormatter;
    public final String defaultReason;
    public final String neverExpires;

    public final TextNode banChatMessage;
    public final TextNode tempBanChatMessage;
    public final TextNode ipBanChatMessage;
    public final TextNode tempIpBanChatMessage;
    public final TextNode muteChatMessage;
    public final TextNode tempMuteChatMessage;
    public final TextNode warnChatMessage;
    public final TextNode tempWarnChatMessage;

    public final TextNode banScreenMessage;
    public final TextNode tempBanScreenMessage;
    public final TextNode ipBanScreenMessage;
    public final TextNode tempIpBanScreenMessage;

    public final TextNode kickScreenMessage;

    public final TextNode kickChatMessage;
    public final ConfigData configData;
    private final Map<String, Long> tempDurationLimit = new HashMap<>();
    public final long defaultDurationLimit;
    public final TextNode pardonChatMessage;
    public final TextNode unmuteChatMessage;
    public final TextNode unbanChatMessage;
    public final TextNode ipUnbanChatMessage;
    public final DiscordMessageData discordMessages;
    public final List<URI> webhooks;
    public final TextNode unwarnChatMessage;

    public Config(ConfigData data, MessageConfigData mData, DiscordMessageData discordMessages) {
        this.discordMessages = discordMessages;
        
        this.mutedMessage = toSingleString(mData.mutedText);
        this.tempMutedMessage = toSingleString(mData.tempMutedText);

        this.banScreenMessage = toSingleString(mData.banScreen);
        this.tempBanScreenMessage = toSingleString(mData.tempBanScreen);

        this.ipBanScreenMessage = toSingleString(mData.ipBanScreen);
        this.tempIpBanScreenMessage = toSingleString(mData.tempIpBanScreen);

        this.kickScreenMessage = toSingleString(mData.kickScreen);
        this.mutedCommands = data.muteBlockedCommands;
        this.dateTimeFormatter = new SimpleDateFormat(mData.dateFormat);
        this.defaultReason = mData.defaultReason;
        this.neverExpires = mData.neverExpiresText;
        this.banChatMessage = toSingleString(mData.banChatMessage);
        this.tempBanChatMessage = toSingleString(mData.tempBanChatMessage);

        this.ipBanChatMessage = toSingleString(mData.ipBanChatMessage);
        this.tempIpBanChatMessage = toSingleString(mData.tempIpBanChatMessage);

        this.muteChatMessage = toSingleString(mData.muteChatMessage);
        this.tempMuteChatMessage = toSingleString(mData.tempMuteChatMessage);
        this.kickChatMessage = toSingleString(mData.kickChatMessage);

        this.unbanChatMessage = toSingleString(mData.unbanChatMessage);
        this.ipUnbanChatMessage = toSingleString(mData.ipUnbanChatMessage);
        this.unmuteChatMessage = toSingleString(mData.unmuteChatMessage);
        this.pardonChatMessage = toSingleString(mData.pardonChatMessage);

        this.warnChatMessage = toSingleString(mData.warnChatMessage);
        this.tempWarnChatMessage = toSingleString(mData.tempWarnChatMessage);
        this.unwarnChatMessage = toSingleString(mData.unwarnChatMessage);

        long dur = BHUtils.parseDuration(data.defaultTempPunishmentDurationLimit);

        this.defaultDurationLimit = dur == -1 ? Long.MAX_VALUE : dur;

        for (Map.Entry<String, String> x : data.permissionTempLimit.entrySet() ) {
            this.tempDurationLimit.put(x.getKey(), BHUtils.parseDuration(x.getValue()));
        }

        this.messageConfigData = mData;
        this.configData = data;
        this.webhooks = new ArrayList<>();
        if (!configData.discordWebhookUrls.isEmpty()) {
            for (var url : configData.discordWebhookUrls) {
                if (url != null && !url.isEmpty()) {
                    try {
                        this.webhooks.add(URI.create(url));
                    } catch (Throwable e) {
                        BanHammerImpl.LOGGER.error("Could use webhook!");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private TextNode toSingleString(List<String> text) {
        if (text.size() == 1) {
            return TextParserUtils.formatNodes(text.get(0));
        } else {
            return TextParserUtils.formatNodes(String.join("\n", text));
        }
    }

    public long getDurationLimit(CommandSource source) {
        long out = 0;
        boolean custom = false;

        for (Map.Entry<String, Long> x : this.tempDurationLimit.entrySet()) {
            if (Permissions.check(source, "banhammer.duration." + x.getKey()) && out < x.getValue()) {
                out = x.getValue();
                custom = true;
            }
        }

        return custom ? out : this.defaultDurationLimit;
    }

    public boolean canPunish(GameProfile profile) {
        return !(this.configData.blockPunishments.contains(profile.getName()) || this.configData.blockPunishments.contains(profile.getId().toString()));
    }

    public DbConfig getDatabaseConfig(String type) {
        var dbConfig = new DbConfig();
        var key = "PB_BANHAMMER_" + type.toUpperCase(Locale.ROOT) + "_";
        dbConfig.address = envOrVal(key + "ADDRESS", configData.databaseAddress);
        dbConfig.database = envOrVal(key + "DATABASE", configData.databaseName);
        dbConfig.username = envOrVal(key + "USERNAME", configData.databaseUsername);
        dbConfig.password = envOrVal(key + "PASSWORD", configData.databasePassword);
        return dbConfig;
    }

    private String envOrVal(String env, String val) {
        return System.getenv().getOrDefault(env, val);
    }
}
