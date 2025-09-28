package eu.pb4.banhammer.impl;

import com.mojang.authlib.GameProfile;
import eu.pb4.placeholders.api.PlaceholderContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record BHPlayerData(GameProfile gameProfile, String ip, Text displayName, @Nullable ServerPlayerEntity player) {
    public UUID uuid() {
        return this.gameProfile.id() == null ? Util.NIL_UUID : this.gameProfile.id();
    }

    public String name() {
        return this.gameProfile.name() == null ? "??: " + this.uuid() : this.gameProfile.name();
    }

    public PlaceholderContext placeholderContext(MinecraftServer server) {
        return player != null ? PlaceholderContext.of(player) : PlaceholderContext.of(gameProfile, server);
    }
}
