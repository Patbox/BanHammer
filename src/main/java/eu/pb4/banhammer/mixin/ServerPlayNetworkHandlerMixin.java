package eu.pb4.banhammer.mixin;

import eu.pb4.banhammer.BanHammerMod;
import eu.pb4.banhammer.Helpers;
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
    private void checkIfMuted(TextStream.Message message, CallbackInfo ci) {
        String string = message.getRaw();
        if (BanHammerMod.isPlayerPunished(this.player.getUuid().toString(), PunishmentTypes.MUTE)) {
            if (string.startsWith("/") && string.length() > 1) {
                int x = string.indexOf(" ");
                String rawCommand = string.substring(1, x != -1 ? x : string.length());
                for (String command : ConfigManager.getConfig().mutedCommands) {
                    if (rawCommand.startsWith(command)) {
                        ci.cancel();
                        BasicPunishment punishment = BanHammerMod.getPlayersPunishments(this.player.getUuid().toString(), PunishmentTypes.MUTE).get(0);
                        this.player.sendMessage(PlaceholderAPI.parsePredefinedText(ConfigManager.getConfig().mutedMessage, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, punishment.getPlaceholders()), false);
                        return;
                    }
                }
            } else {
                BasicPunishment punishment = BanHammerMod.getPlayersPunishments(this.player.getUuid().toString(), PunishmentTypes.MUTE).get(0);
                if (punishment.duration == -1) {
                    this.player.sendMessage(PlaceholderAPI.parsePredefinedText(ConfigManager.getConfig().mutedMessage, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, punishment.getPlaceholders()), false);
                } else {
                    this.player.sendMessage(PlaceholderAPI.parsePredefinedText(ConfigManager.getConfig().tempMutedMessage, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, punishment.getPlaceholders()), false);
                }
                ci.cancel();
            }
        }
    }

}
