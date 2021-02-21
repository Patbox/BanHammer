package eu.pb4.banhammer.mixin;

import eu.pb4.banhammer.BanHammerMod;
import eu.pb4.banhammer.Helpers;
import eu.pb4.banhammer.config.ConfigManager;
import eu.pb4.banhammer.types.BasicPunishment;
import eu.pb4.banhammer.types.PunishmentTypes;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "method_31286", at = @At("HEAD"), cancellable = true)
    private void checkIfMuted(String string, CallbackInfo ci) {
        if (BanHammerMod.isPlayerPunished(this.player.getUuid().toString(), PunishmentTypes.MUTE)) {
            if (string.startsWith("/") && string.length() > 1) {
                int x = string.indexOf(" ");
                String rawCommand = string.substring(1, x != -1 ? x : string.length());
                for (String command : ConfigManager.getConfig().getMutedCommands()) {
                    if (rawCommand.startsWith(command)) {
                        ci.cancel();
                        BasicPunishment punishment = BanHammerMod.getPlayersPunishments(this.player.getUuid().toString(), PunishmentTypes.MUTE).get(0);
                        this.player.sendMessage(Helpers.parseMessage(ConfigManager.getConfig().getMutedMessage(), Helpers.getTemplateFor(punishment)), false);
                        return;
                    }
                }
            } else {
                BasicPunishment punishment = BanHammerMod.getPlayersPunishments(this.player.getUuid().toString(), PunishmentTypes.MUTE).get(0);
                this.player.sendMessage(Helpers.parseMessage(ConfigManager.getConfig().getMutedMessage(), Helpers.getTemplateFor(punishment)), false);
                ci.cancel();
            }
        }
    }

}
