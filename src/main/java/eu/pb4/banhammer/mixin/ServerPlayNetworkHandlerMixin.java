package eu.pb4.banhammer.mixin;

import eu.pb4.banhammer.api.PunishmentType;
import eu.pb4.banhammer.impl.BanHammerImpl;
import eu.pb4.banhammer.impl.config.ConfigManager;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onCommandExecution", at = @At("HEAD"), cancellable = true)
    private void banHammer_checkIfMutedCommand(CommandExecutionC2SPacket packet, CallbackInfo ci) {
        for (var punishment : BanHammerImpl.CACHED_PUNISHMENTS) {
            if (!punishment.isExpired() && punishment.type == PunishmentType.MUTE && punishment.playerUUID.equals(this.player.getUuid())) {
                this.player.sendMessage(punishment.getDisconnectMessage(), false);
                ci.cancel();
                return;
            }
        }

        var punishments = BanHammerImpl.getPlayersPunishments(this.player.getUuid().toString(), PunishmentType.MUTE);
        var string = packet.command();
        if (punishments.size() > 0) {
            var punishment = punishments.get(0);

            int x = string.indexOf(" ");
            String rawCommand = string.substring(0, x != -1 ? x : string.length());
            for (String command : ConfigManager.getConfig().mutedCommands) {
                if (rawCommand.startsWith(command)) {
                    ci.cancel();
                    this.player.sendMessage(punishment.getDisconnectMessage(), false);
                    return;
                }
            }
        }
    }
}
