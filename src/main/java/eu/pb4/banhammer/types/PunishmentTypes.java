package eu.pb4.banhammer.types;

public enum PunishmentTypes {
    BAN("ban", true, false, "bans"),
    IPBAN("ipban",true, true, "ipbans"),
    MUTE("mute",false, false, "mutes"),
    KICK("kick", true, false, null);


    public final boolean ipBased;
    public final boolean kick;
    public final String databaseName;
    public final String name;

    PunishmentTypes(String name, boolean shouldKick, boolean ipBased, String databaseName) {
        this.name = name;
        this.ipBased = ipBased;
        this.kick = shouldKick;
        this.databaseName = databaseName;
    }
}
