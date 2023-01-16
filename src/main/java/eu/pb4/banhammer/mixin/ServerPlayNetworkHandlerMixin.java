package eu.pb4.banhammer.mixin;

import eu.pb4.banhammer.impl.BanHammerImpl;
import eu.pb4.banhammer.impl.config.ConfigManager;
import eu.pb4.banhammer.api.PunishmentType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.play.MessageHeaderS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.filter.FilteredMessage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;

    @Shadow @Final private MinecraftServer server;

    @Shadow protected abstract SignedMessage getSignedMessage(ChatMessageC2SPacket packet);

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    private void banHammer_checkIfMuted(ChatMessageC2SPacket packet, CallbackInfo ci) {
        for (var punishment : BanHammerImpl.CACHED_PUNISHMENTS) {
            if (!punishment.isExpired() && punishment.type == PunishmentType.MUTE && punishment.playerUUID.equals(this.player.getUuid())) {
                this.player.sendMessage(punishment.getDisconnectMessage(), false);

                if (!packet.signature().isEmpty()) {
                    this.server.getPlayerManager().sendMessageHeader(this.getSignedMessage(packet), Set.of());
                }
                ci.cancel();
                return;
            }
        }

        var punishments = BanHammerImpl.getPlayersPunishments(this.player.getUuid().toString(), PunishmentType.MUTE);

        if (punishments.size() > 0) {
            var punishment = punishments.get(0);
            this.player.sendMessage(punishment.getDisconnectMessage(), false);

            if (!packet.signature().isEmpty()) {
                this.server.getPlayerManager().sendMessageHeader(this.getSignedMessage(packet), Set.of());
            }
            ci.cancel();
        }
    }

    @Inject(method = "onCommandExecution", at = @At("HEAD"), cancellable = true)
    private void banHammer_checkIfMutedCommand(CommandExecutionC2SPacket packet, CallbackInfo ci) {
        var string = packet.command();
        int x = string.indexOf(" ");
        String rawCommand = string.substring(0, x != -1 ? x : string.length());
        for (String command : ConfigManager.getConfig().mutedCommands) {
            if (rawCommand.equals(command)) {
                for (var punishment : BanHammerImpl.CACHED_PUNISHMENTS) {
                    if (!punishment.isExpired() && punishment.type == PunishmentType.MUTE && punishment.playerUUID.equals(this.player.getUuid())) {
                        this.player.sendMessage(punishment.getDisconnectMessage(), false);
                        ci.cancel();
                        return;
                    }
                }

                var punishments = BanHammerImpl.getPlayersPunishments(this.player.getUuid().toString(), PunishmentType.MUTE);

                if (punishments.size() > 0) {
                    var punishment = punishments.get(0);


                    ci.cancel();
                    this.player.sendMessage(punishment.getDisconnectMessage(), false);
                }

                return;
            }
        }
    }
}
