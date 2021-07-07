package eu.pb4.banhammer.config;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.AllowedMentions;
import eu.pb4.banhammer.BanHammerMod;
import eu.pb4.banhammer.Helpers;
import eu.pb4.banhammer.config.data.ConfigData;
import eu.pb4.banhammer.config.data.DiscordMessageData;
import eu.pb4.banhammer.config.data.MessageConfigData;
import eu.pb4.placeholders.TextParser;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    public final List<String> mutedCommands;
    public final MessageConfigData messageConfigData;

    public final Text mutedMessage;
    public final Text tempMutedMessage;

    public final SimpleDateFormat dateTimeFormatter;
    public final String defaultReason;
    public final String neverExpires;

    public final Text banChatMessage;
    public final Text tempBanChatMessage;
    public final Text ipBanChatMessage;
    public final Text tempIpBanChatMessage;
    public final Text muteChatMessage;
    public final Text tempMuteChatMessage;

    public final Text banScreenMessage;
    public final Text tempBanScreenMessage;
    public final Text ipBanScreenMessage;
    public final Text tempIpBanScreenMessage;

    public final Text kickScreenMessage;

    public final Text kickChatMessage;
    public final ConfigData configData;
    private final Map<String, Long> tempDurationLimit = new HashMap<>();
    public final long defaultDurationLimit;
    public final Text pardonChatMessage;
    public final Text unmuteChatMessage;
    public final Text unbanChatMessage;
    public final Text ipUnbanChatMessage;
    public final DiscordMessageData discordMessages;
    public final WebhookClient webhook;


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

        long dur = Helpers.parseDuration(data.defaultTempPunishmentDurationLimit);

        this.defaultDurationLimit = dur == -1 ? Long.MAX_VALUE : dur;

        for (Map.Entry<String, String> x : data.permissionTempLimit.entrySet() ) {
            this.tempDurationLimit.put(x.getKey(), Helpers.parseDuration(x.getValue()));
        }

        this.messageConfigData = mData;
        this.configData = data;

        if (!configData.discordWebhookUrl.isEmpty()) {
            WebhookClient client;
            try {
                WebhookClientBuilder builder = new WebhookClientBuilder(configData.discordWebhookUrl);
                builder.setHttpClient(new OkHttpClient.Builder()
                        .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                        .build());
                builder.setDaemon(true);
                builder.setAllowedMentions(AllowedMentions.none());
                client = builder.build();
            } catch (Exception e) {
                BanHammerMod.LOGGER.error("Could use webhook!");
                e.printStackTrace();
                client = null;
            }
            this.webhook = client;
        } else {
            this.webhook = null;
        }
    }

    private Text toSingleString(List<String> text) {
        if (text.size() == 1) {
            return TextParser.parse(text.get(0));
        } else {
            return TextParser.parse(String.join("\n", text));
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

    public void destroy() {}

}
