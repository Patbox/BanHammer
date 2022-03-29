package eu.pb4.banhammer.impl.config.data;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MessageConfigData {
    public List<String> banChatMessage = Arrays.asList("Player <red>${banned}</red> has been banned by <gold>${operator}</gold>!",
            "Reason: <yellow>${reason}</yellow>");

    public List<String> tempBanChatMessage = Arrays.asList("Player <red>${banned}</red> has been banned by <gold>${operator}</gold>!",
            "Reason: <yellow>${reason}</yellow>. Expires in: <yellow>${expiration_time}</yellow>");

    public List<String> ipBanChatMessage = Arrays.asList("Player <red>${banned}</red> has been banned by <gold>${operator}</gold>!",
            "Reason: <yellow>${reason}</yellow>");

    public List<String> tempIpBanChatMessage = Arrays.asList("Player <red>${banned}</red> has been banned by <gold>${operator}</gold>!",
            "Reason: <yellow>${reason}</yellow>. Expires in: <yellow>${expiration_time}</yellow>");

    public List<String> muteChatMessage = Arrays.asList("Player <red>${banned}</red> has been muted by <gold>${operator}</gold>!",
            "Reason: <yellow>${reason}</yellow>");

    public List<String> tempMuteChatMessage = Arrays.asList("Player <red>${banned}</red> has been muted by <gold>${operator}</gold>!",
            "Reason: <yellow>${reason}</yellow>. Expires in: <yellow>${expiration_time}</yellow>");

    public List<String> kickChatMessage = Arrays.asList("Player <red>${banned}</red> has been kicked by <gold>${operator}</gold>!",
            "Reason: <yellow>${reason}</yellow>");

    public List<String> warnChatMessage = Arrays.asList("Player <red>${banned}</red> has been warned by <gold>${operator}</gold>!",
            "Reason: <yellow>${reason}</yellow>");

    public List<String> tempWarnChatMessage = Arrays.asList("Player <red>${banned}</red> has been warned by <gold>${operator}</gold>!",
            "Reason: <yellow>${reason}</yellow>. Expires in: <yellow>${expiration_time}</yellow>");

    public List<String> unbanChatMessage = Arrays.asList("Player <red>${banned}</red> has been unbanned by <gold>${operator}</gold>!", "Reason: <yellow>${reason}</yellow>");

    public List<String> ipUnbanChatMessage = Arrays.asList("Player <red>${banned}</red> has been unbanned by <gold>${operator}</gold>!", "Reason: <yellow>${reason}</yellow>");

    public List<String> unmuteChatMessage = Arrays.asList("Player <red>${banned}</red> has been unmuted by <gold>${operator}</gold>!", "Reason: <yellow>${reason}</yellow>");

    public List<String> unwarnChatMessage = Arrays.asList("Player <red>${banned}</red>'s warnings has been removed by <gold>${operator}</gold>!", "Reason: <yellow>${reason}</yellow>");

    public List<String> pardonChatMessage = Arrays.asList("Punishments of player <red>${banned}</red> has been redeemed by <gold>${operator}</gold>!", "Reason: <yellow>${reason}</yellow>");

    public List<String> banScreen = Arrays.asList("<red><bold>You are banned</bold></red>",
            "<gray>Reason: </gray><yellow>${reason}</yellow>",
            "<gray>By: </gray><yellow>${operator}</yellow>");

    public List<String> tempBanScreen = Arrays.asList("<red><bold>You are banned</bold></red>",
            "<gray>Reason: </gray><yellow>${reason}</yellow>",
            "<gray>Expires in: </gray><yellow>${expiration_time}</yellow>",
            "<gray>By: </gray><yellow>${operator}</yellow>");

    public List<String> ipBanScreen = Arrays.asList("<red><bold>You are banned</bold></red>",
            "<gray>Reason: </gray><yellow>${reason}</yellow>",
            "<gray>By: </gray><yellow>${operator}</yellow>");

    public List<String> tempIpBanScreen = Arrays.asList("<red><bold>You are banned</bold></red>",
            "<gray>Reason: </gray><yellow>${reason}</yellow>",
            "<gray>Expires in: </gray><yellow>${expiration_time}</yellow>",
            "<gray>By: </gray><yellow>${operator}</yellow>");

    public List<String> kickScreen = Arrays.asList("<red><bold>You has been kicked!</bold></red>",
            "<gray>Reason: </gray><yellow>${reason}</yellow>",
            "<gray>By: </gray><yellow>${operator}</yellow>");

    public List<String> mutedText = Collections.singletonList("<red>You are muted by ${operator}. Reason: ${reason}</red>");
    public List<String> tempMutedText = Collections.singletonList("<red>You are muted for ${expiration_time} by ${operator}. Reason: ${reason}</red>");

    public String defaultReason = "Unknown reason";

    public String dateFormat = "dd.MM.YYYY HH:mm";
    public String neverExpiresText = "Never";

    public String yearsText = " year(s) ";
    public String daysText = " day(s) ";
    public String hoursText = " hour(s) ";
    public String minutesText = " minute(s) ";
    public String secondsText = " second(s)";
}
