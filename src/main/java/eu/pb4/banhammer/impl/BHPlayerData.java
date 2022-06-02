package eu.pb4.banhammer.impl;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record BHPlayerData(GameProfile gameProfile, String ip, Text displayName, @Nullable ServerPlayerEntity player) {
    public UUID uuid() {
        return this.gameProfile.getId() == null ? Util.NIL_UUID : this.gameProfile.getId();
    }

    public String name() {
        return this.gameProfile.getName() == null ? "??: " + this.uuid() : this.gameProfile.getName();
    }
}
