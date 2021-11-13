package eu.pb4.banhammer.mixin;

import eu.pb4.banhammer.BanHammer;
import eu.pb4.banhammer.config.ConfigManager;
import eu.pb4.banhammer.types.BasicPunishment;
import eu.pb4.banhammer.types.PunishmentTypes;
import eu.pb4.placeholders.PlaceholderAPI;
import net.minecraft.server.filter.TextStream;
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

    @Inject(method = "handleMessage", at = @At("HEAD"), cancellable = true)
    private void banHammer_checkIfMuted(TextStream.Message message, CallbackInfo ci) {
        String string = message.getRaw();

        var punishments = BanHammer.getPlayersPunishments(this.player.getUuid().toString(), PunishmentTypes.MUTE);

        if (punishments.size() > 0) {
            var punishment = punishments.get(0);

            if (string.startsWith("/") && string.length() > 1) {
                int x = string.indexOf(" ");
                String rawCommand = string.substring(1, x != -1 ? x : string.length());
                for (String command : ConfigManager.getConfig().mutedCommands) {
                    if (rawCommand.startsWith(command)) {
                        ci.cancel();
                        this.player.sendMessage(PlaceholderAPI.parsePredefinedText(punishment.getDisconnectMessage(), PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, punishment.getPlaceholders()), false);
                        return;
                    }
                }
            } else {
                this.player.sendMessage(PlaceholderAPI.parsePredefinedText(punishment.getDisconnectMessage(), PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, punishment.getPlaceholders()), false);
                ci.cancel();
            }
        }
    }

}
