package eu.pb4.banhammer.mixin;

import com.mojang.authlib.GameProfile;
import eu.pb4.banhammer.BanHammerMod;
import eu.pb4.banhammer.Helpers;
import eu.pb4.banhammer.types.BasicPunishment;
import eu.pb4.banhammer.types.PunishmentTypes;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(method = "onPlayerConnect", at = @At("HEAD"))
    private void cachePlayersIP(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        BanHammerMod.IP_CACHE.put(player.getUuid().toString(), Helpers.stringifyAddress(connection.getAddress()));
    }

    @Inject(method = "checkCanJoin", at = @At("TAIL"), cancellable = true)
    private void checkIfBanned(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        BasicPunishment punishment = null;

        if (BanHammerMod.isPlayerPunished(profile.getId().toString(), PunishmentTypes.BAN)) {
            punishment = BanHammerMod.getPlayersPunishments(profile.getId().toString(), PunishmentTypes.BAN).get(0);
        } else if (BanHammerMod.isPlayerPunished(Helpers.stringifyAddress(address), PunishmentTypes.IPBAN)) {
            punishment = BanHammerMod.getPlayersPunishments(Helpers.stringifyAddress(address), PunishmentTypes.IPBAN).get(0);
        }

        if (punishment != null) {
            cir.setReturnValue(punishment.getDisconnectMessage());
        }
    }
}
