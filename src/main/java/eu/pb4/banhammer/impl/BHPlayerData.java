package eu.pb4.banhammer.impl;

import com.mojang.authlib.GameProfile;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record BHPlayerData(@Nullable GameProfile gameProfile, String ip, Component displayName, @Nullable ServerPlayer player) {
    public UUID uuid() {
        return this.gameProfile == null || this.gameProfile.id() == null ? Util.NIL_UUID : this.gameProfile.id();
    }

    public String name() {
        return this.gameProfile == null || this.gameProfile.name() == null ? "??: " + this.uuid() : this.gameProfile.name();
    }

    public ServerPlaceholderContext placeholderContext(MinecraftServer server) {
        return player != null ? ServerPlaceholderContext.of(player) : (gameProfile != null ? ServerPlaceholderContext.of(gameProfile, server) : ServerPlaceholderContext.of(server));
    }
}
