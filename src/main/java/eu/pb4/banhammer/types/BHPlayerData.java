package eu.pb4.banhammer.types;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record BHPlayerData(UUID uuid, String name, String ip, Text displayName, @Nullable ServerPlayerEntity player) { }
