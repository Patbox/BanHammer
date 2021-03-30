package eu.pb4.banhammer.types;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public class BHPlayerData {
    public final UUID uuid;
    public final String name;
    public final String ip;
    public final Text displayName;
    public final ServerPlayerEntity player;

    public BHPlayerData(UUID uuid, String name, String ip, Text displayName, ServerPlayerEntity player) {
        this.uuid = uuid;
        this.name = name;
        this.ip = ip;
        this.displayName = displayName;
        this.player = player;
    }
}
