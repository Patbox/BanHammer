package eu.pb4.banhammer.api;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum PunishmentType {
    BAN("ban", true, false, "bans"),
    IP_BAN("ipban", true, true, "ipbans"),
    MUTE("mute", false, false, "mutes"),
    KICK("kick", true, false, null),
    WARN("warn", false, false, "warns");


    private static final Map<String, PunishmentType> BY_NAME = Arrays.stream(PunishmentType.values()).collect(Collectors.toMap(type -> type.name, type -> type));

    public final boolean ipBased;
    public final boolean kick;
    @Nullable
    public final String databaseName;
    public final String name;

    PunishmentType(String name, boolean shouldKick, boolean ipBased, String databaseName) {
        this.name = name;
        this.ipBased = ipBased;
        this.kick = shouldKick;
        this.databaseName = databaseName;
    }

    public static PunishmentType fromName(String type) {
        return switch (type) {
            case "ban" -> BAN;
            case "ipban" -> IP_BAN;
            case "mute" -> MUTE;
            case "warn" -> WARN;
            default -> KICK;
        };
    }

    public boolean useDatabase() {
        return this.databaseName != null;
    }
}
