package eu.pb4.banhammer.mixin;

import com.mojang.authlib.GameProfile;
import eu.pb4.banhammer.impl.BHUtils;
import eu.pb4.banhammer.impl.BanHammerImpl;
import eu.pb4.banhammer.impl.config.ConfigManager;
import eu.pb4.banhammer.api.PunishmentData;
import eu.pb4.banhammer.api.PunishmentType;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(method = "checkCanJoin", at = @At("HEAD"))
    private void banHammer_cachePlayersIP(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        if (address != null) {
            var stringAddress = BHUtils.stringifyAddress(address);

            BanHammerImpl.UUID_TO_IP_CACHE.put(profile.getId(), stringAddress);
            BanHammerImpl.IP_TO_UUID_CACHE.computeIfAbsent(stringAddress, (ip) -> new HashSet<>()).add(profile.getId());
        }
    }

    @Inject(method = "checkCanJoin", at = @At("TAIL"), cancellable = true)
    private void banHammer_checkIfBanned(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        PunishmentData punishment = null;

        if (address == null || profile == null) {
            return;
        }

        String ip = BHUtils.stringifyAddress(address);

        for (var pos : BanHammerImpl.CACHED_PUNISHMENTS) {
            if (!pos.isExpired() && ((pos.type == PunishmentType.IP_BAN && pos.playerIP.equals(ip))
                    || (pos.type == PunishmentType.BAN && pos.playerUUID.equals(profile.getId())))) {
                punishment = pos;
                break;
            }
        }


        if (punishment == null) {
            final var bans = BanHammerImpl.getPlayersPunishments(profile.getId().toString(), PunishmentType.BAN);
            final var ipBans = BanHammerImpl.getPlayersPunishments(ip, PunishmentType.IP_BAN);

            if (bans.size() > 0) {
                punishment = bans.get(0);
            } else if (ipBans.size() > 0) {
                punishment = ipBans.get(0);
            }
        }

        if (punishment != null) {
            if (punishment.type == PunishmentType.IP_BAN && ConfigManager.getConfig().configData.standardBanPlayersWithBannedIps) {
                final boolean silent = ConfigManager.getConfig().configData.autoBansFromIpBansAreSilent;

                PunishmentData punishment1 = new PunishmentData(profile.getId(), BHUtils.stringifyAddress(address), Text.literal(profile.getName()), profile.getName(),
                        punishment.adminUUID,
                        punishment.adminDisplayName,
                        punishment.time,
                        punishment.duration,
                        punishment.reason,
                        PunishmentType.BAN);

                BanHammerImpl.punishPlayer(punishment1, silent, silent);
            }
            cir.setReturnValue(punishment.getDisconnectMessage());
        }
    }
}
