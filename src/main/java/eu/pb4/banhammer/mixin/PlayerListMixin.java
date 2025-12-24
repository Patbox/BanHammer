package eu.pb4.banhammer.mixin;

import com.mojang.authlib.GameProfile;
import eu.pb4.banhammer.impl.BHUtils;
import eu.pb4.banhammer.impl.BanHammerImpl;
import eu.pb4.banhammer.impl.config.ConfigManager;
import eu.pb4.banhammer.api.PunishmentData;
import eu.pb4.banhammer.api.PunishmentType;
import eu.pb4.placeholders.api.PlaceholderContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.HashSet;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "canPlayerLogin", at = @At("HEAD"))
    private void banHammer_cachePlayersIP(SocketAddress address, NameAndId profile, CallbackInfoReturnable<Component> cir) {
        if (address != null) {
            var stringAddress = BHUtils.stringifyAddress(address);

            BanHammerImpl.UUID_TO_IP_CACHE.put(profile.id(), stringAddress);
            BanHammerImpl.IP_TO_UUID_CACHE.computeIfAbsent(stringAddress, (ip) -> new HashSet<>()).add(profile.id());
        }
    }

    @Inject(method = "canPlayerLogin", at = @At("TAIL"), cancellable = true)
    private void banHammer_checkIfBanned(SocketAddress address, NameAndId profile, CallbackInfoReturnable<Component> cir) {
        PunishmentData punishment = null;

        if (address == null || profile == null) {
            return;
        }

        String ip = BHUtils.stringifyAddress(address);

        for (var pos : BanHammerImpl.CACHED_PUNISHMENTS) {
            if (!pos.isExpired() && ((pos.type == PunishmentType.IP_BAN && pos.playerIP.equals(ip))
                    || (pos.type == PunishmentType.BAN && pos.playerUUID.equals(profile.id())))) {
                punishment = pos;
                break;
            }
        }


        if (punishment == null) {
            final var bans = BanHammerImpl.getPlayersPunishments(profile.id().toString(), PunishmentType.BAN);
            final var ipBans = BanHammerImpl.getPlayersPunishments(ip, PunishmentType.IP_BAN);

            if (!bans.isEmpty()) {
                punishment = bans.getFirst();
            } else if (!ipBans.isEmpty()) {
                punishment = ipBans.getFirst();
            }
        }

        if (punishment != null) {
            if (punishment.type == PunishmentType.IP_BAN && ConfigManager.getConfig().configData.standardBanPlayersWithBannedIps) {
                final boolean silent = ConfigManager.getConfig().configData.autoBansFromIpBansAreSilent;

                PunishmentData punishment1 = new PunishmentData(profile.id(), BHUtils.stringifyAddress(address), Component.literal(profile.name()), profile.name(),
                        punishment.adminUUID,
                        punishment.adminDisplayName,
                        punishment.time,
                        punishment.duration,
                        punishment.reason,
                        PunishmentType.BAN);

                BanHammerImpl.punishPlayer(punishment1, silent, silent);
            }
            cir.setReturnValue(punishment.getDisconnectMessage(PlaceholderContext.of(new GameProfile(profile.id(), profile.name()), server)));
        }
    }
}
