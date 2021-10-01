package eu.pb4.banhammer.mixin;

import com.mojang.authlib.GameProfile;
import eu.pb4.banhammer.BanHammer;
import eu.pb4.banhammer.Helpers;
import eu.pb4.banhammer.config.ConfigManager;
import eu.pb4.banhammer.types.BasicPunishment;
import eu.pb4.banhammer.types.PunishmentTypes;
import eu.pb4.banhammer.types.SyncedPunishment;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.List;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(method = "checkCanJoin", at = @At("HEAD"))
    private void banHammer_cachePlayersIP(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        if (address != null) {
            var stringAddress = Helpers.stringifyAddress(address);

            BanHammer.UUID_TO_IP_CACHE.put(profile.getId(), stringAddress);
            BanHammer.IP_TO_UUID_CACHE.put(stringAddress, profile.getId());
        }
    }

    @Inject(method = "checkCanJoin", at = @At("TAIL"), cancellable = true)
    private void banHammer_checkIfBanned(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        BasicPunishment punishment = null;

        if (address == null || profile == null) {
            return;
        }

        String ip = Helpers.stringifyAddress(address);

        final List<SyncedPunishment> bans = BanHammer.getPlayersPunishments(profile.getId().toString(), PunishmentTypes.BAN);
        final List<SyncedPunishment> ipBans = BanHammer.getPlayersPunishments(ip, PunishmentTypes.IPBAN);

        if (bans.size() > 0) {
            punishment = bans.get(0);
        } else if (ipBans.size() > 0) {
            punishment = ipBans.get(0);
        }

        if (punishment != null) {
            if (punishment.type == PunishmentTypes.IPBAN && ConfigManager.getConfig().configData.standardBanPlayersWithBannedIps) {
                final boolean silent = ConfigManager.getConfig().configData.autoBansFromIpBansAreSilent;

                BasicPunishment punishment1 = new BasicPunishment(profile.getId(), Helpers.stringifyAddress(address), new LiteralText(profile.getName()), profile.getName(),
                        punishment.adminUUID,
                        punishment.adminDisplayName,
                        punishment.time,
                        punishment.duration,
                        punishment.reason,
                        PunishmentTypes.BAN);

                BanHammer.punishPlayer(punishment1, silent, silent);
            }
            cir.setReturnValue(punishment.getDisconnectMessage());
        }
    }
}
