package eu.pb4.banhammer.config;

import eu.pb4.banhammer.Helpers;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private final List<String> mutedCommands;
    private final MessageConfigData messageConfigData;
    private final String mutedMessage;
    private final String banScreenMessage;
    private final SimpleDateFormat dateTimeFormatter;
    private final String defaultReason;
    private final String neverExpires;
    private final String banChatMessage;
    private final String muteChatMessage;
    private final String kickChatMessage;
    private final ConfigData configData;
    private final Map<String, Long> tempDurationLimit = new HashMap<>();
    private final long defaultDurationLimit;
    private final String kickScreenMessage;
    private final String pardonChatMessage;
    private final String unmuteChatMessage;
    private final String unbanChatMessage;


    public Config(ConfigData data, MessageConfigData mData) {
        this.mutedMessage = String.join("\n", mData.mutedText);
        this.banScreenMessage = String.join("\n", mData.bannedScreen);
        this.kickScreenMessage = String.join("\n", mData.kickScreen);
        this.mutedCommands = data.muteBlockedCommands;
        this.dateTimeFormatter = new SimpleDateFormat(mData.dateFormat);
        this.defaultReason = mData.defaultReason;
        this.neverExpires = mData.neverExpiresText;
        this.banChatMessage = String.join("\n", mData.banChatMessage);
        this.muteChatMessage = String.join("\n", mData.muteChatMessage);
        this.kickChatMessage = String.join("\n", mData.kickChatMessage);

        this.unbanChatMessage = String.join("\n", mData.unbanChatMessage);
        this.unmuteChatMessage = String.join("\n", mData.unmuteChatMessage);
        this.pardonChatMessage = String.join("\n", mData.pardonChatMessage);

        this.defaultDurationLimit = Helpers.parseDuration(data.defaultTempPunishmentDurationLimit);

        for (Map.Entry<String, String> x : data.permissionTempLimit.entrySet() ) {
            this.tempDurationLimit.put(x.getKey(), Long.valueOf(Helpers.parseDuration(x.getValue())));
        }

        this.messageConfigData = mData;
        this.configData = data;
    }

    public String getMutedMessage() {
        return this.mutedMessage;
    }

    public String getMuteChatMessage() {
        return this.muteChatMessage;
    }

    public String getBanChatMessage() {
        return this.banChatMessage;
    }

    public String getKickChatMessage() {
        return this.kickChatMessage;
    }

    public String getUnmuteChatMessage() {
        return this.unmuteChatMessage;
    }

    public String getUnbanChatMessage() {
        return this.unbanChatMessage;
    }

    public String getPardonChatMessage() {
        return this.pardonChatMessage;
    }

    public String getBanScreenMessage() {
        return this.banScreenMessage;
    }

    public String getKickScreenMessage() { return this.kickScreenMessage; }

    public SimpleDateFormat getDateFormatter() {
        return this.dateTimeFormatter;
    }

    public ConfigData getConfigData() {
        return this.configData;
    }

    public MessageConfigData getMessageConfigData() {
        return this.messageConfigData;
    }

    public List<String> getMutedCommands() {
        return this.mutedCommands;
    }

    public String getDefaultReason() { return this.defaultReason; }

    public String getNeverExpires() { return this.neverExpires; }

    public long getDurationLimit(CommandSource source) {
        long out = 0;
        boolean overriden = false;

        for (Map.Entry<String, Long> x : this.tempDurationLimit.entrySet()) {
            if (Permissions.check(source, x.getKey()) && out < x.getValue()) {
                out = x.getValue();
            }
        }

        return overriden ? out : this.defaultDurationLimit;
    }

}
