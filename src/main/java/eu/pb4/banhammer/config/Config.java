package eu.pb4.banhammer.config;

import eu.pb4.banhammer.Helpers;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    public final List<String> mutedCommands;
    public final MessageConfigData messageConfigData;
    public final String mutedMessage;
    public final String banScreenMessage;
    public final SimpleDateFormat dateTimeFormatter;
    public final String defaultReason;
    public final String neverExpires;
    public final String banChatMessage;
    public final String muteChatMessage;
    public final String kickChatMessage;
    public final ConfigData configData;
    private final Map<String, Long> tempDurationLimit = new HashMap<>();
    public final long defaultDurationLimit;
    public final String kickScreenMessage;
    public final String pardonChatMessage;
    public final String unmuteChatMessage;
    public final String unbanChatMessage;
    public final String ipUnbanChatMessage;
    public final String ipBanChatMessage;
    public final String ipBanScreenMessage;

    public Config(ConfigData data, MessageConfigData mData) {
        this.mutedMessage = toSingleString(mData.mutedText);
        this.banScreenMessage = toSingleString(mData.banScreen);
        this.ipBanScreenMessage = toSingleString(mData.ipBanScreen);
        this.kickScreenMessage = toSingleString(mData.kickScreen);
        this.mutedCommands = data.muteBlockedCommands;
        this.dateTimeFormatter = new SimpleDateFormat(mData.dateFormat);
        this.defaultReason = mData.defaultReason;
        this.neverExpires = mData.neverExpiresText;
        this.banChatMessage = toSingleString(mData.banChatMessage);
        this.ipBanChatMessage = toSingleString(mData.ipBanChatMessage);
        this.muteChatMessage = toSingleString(mData.muteChatMessage);
        this.kickChatMessage = toSingleString(mData.kickChatMessage);

        this.unbanChatMessage = toSingleString(mData.unbanChatMessage);
        this.ipUnbanChatMessage = toSingleString(mData.ipUnbanChatMessage);
        this.unmuteChatMessage = toSingleString(mData.unmuteChatMessage);
        this.pardonChatMessage = toSingleString(mData.pardonChatMessage);

        this.defaultDurationLimit = Helpers.parseDuration(data.defaultTempPunishmentDurationLimit);

        for (Map.Entry<String, String> x : data.permissionTempLimit.entrySet() ) {
            this.tempDurationLimit.put(x.getKey(), Helpers.parseDuration(x.getValue()));
        }

        this.messageConfigData = mData;
        this.configData = data;
    }

    private String toSingleString(List<String> text) {
        if (text.size() == 1) {
            return text.get(0);
        } else {
            return String.join("\n", text);
        }
    }

    public long getDurationLimit(CommandSource source) {
        long out = 0;
        boolean custom = false;

        for (Map.Entry<String, Long> x : this.tempDurationLimit.entrySet()) {
            if (Permissions.check(source, x.getKey()) && out < x.getValue()) {
                out = x.getValue();
                custom = true;
            }
        }

        return custom ? out : this.defaultDurationLimit;
    }

}
