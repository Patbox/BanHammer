package eu.pb4.banhammer.imports;

import com.mojang.authlib.GameProfile;
import eu.pb4.banhammer.BanHammerMod;
import eu.pb4.banhammer.mixin.accessor.ServerConfigEntryAccessor;
import eu.pb4.banhammer.types.BasicPunishment;
import eu.pb4.banhammer.types.PunishmentTypes;
import net.minecraft.server.BannedIpEntry;
import net.minecraft.server.BannedIpList;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;

public class VanillaImport implements BanHammerMod.PunishmentImporter {
    public boolean importPunishments(boolean remove) {
        try {
            BannedPlayerList banList = BanHammerMod.SERVER.getPlayerManager().getUserBanList();
            BannedIpList ipBanList = BanHammerMod.SERVER.getPlayerManager().getIpBanList();

            for (BannedPlayerEntry data : banList.values()) {
                GameProfile profile = ((ServerConfigEntryAccessor<GameProfile>) data).getKeyServer();

                long creation = data    .getCreationDate().getTime() / 1000;
                long expiration;

                try {
                    expiration = data.getExpiryDate().getTime() / 1000 - creation;
                } catch (Exception e) {
                    expiration = -1;
                }

                BasicPunishment punishment = new BasicPunishment(
                        profile.getId(),
                        "undefined",
                        new LiteralText(profile.getName()),
                        profile.getName(),
                        Util.NIL_UUID,
                        new LiteralText(data.getSource()),
                        creation,
                        expiration,
                        data.getReason(),
                        PunishmentTypes.BAN);

                BanHammerMod.punishPlayer(punishment, true, true);

                if (remove) {
                    banList.remove(data);
                }
            }

            for (BannedIpEntry data : ipBanList.values()) {
                String ip = ((ServerConfigEntryAccessor<String>) data).getKeyServer();

                BasicPunishment punishment = new BasicPunishment(
                        Util.NIL_UUID,
                        ip,
                        new LiteralText("Unknown player"),
                        "Unknown player",
                        Util.NIL_UUID,
                        new LiteralText(data.getSource()),
                        System.currentTimeMillis() / 1000,
                        data.getExpiryDate().getTime() - System.currentTimeMillis() / 1000,
                        data.getReason(),
                        PunishmentTypes.IPBAN);

                BanHammerMod.punishPlayer(punishment, true, true);

                if (remove) {
                    ipBanList.remove(data);
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}