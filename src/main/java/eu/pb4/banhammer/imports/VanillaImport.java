package eu.pb4.banhammer.imports;

import com.mojang.authlib.GameProfile;
import eu.pb4.banhammer.BanHammer;
import eu.pb4.banhammer.mixin.accessor.ServerConfigEntryAccessor;
import eu.pb4.banhammer.types.BasicPunishment;
import eu.pb4.banhammer.types.PunishmentTypes;
import net.minecraft.server.BannedIpEntry;
import net.minecraft.server.BannedIpList;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;

public class VanillaImport implements BanHammer.PunishmentImporter {
    public boolean importPunishments(boolean remove) {
        try {
            BannedPlayerList banList = BanHammer.SERVER.getPlayerManager().getUserBanList();
            BannedIpList ipBanList = BanHammer.SERVER.getPlayerManager().getIpBanList();

            for (BannedPlayerEntry data : banList.values()) {
                try {
                    GameProfile profile = ((ServerConfigEntryAccessor<GameProfile>) data).getKeyServer();

                    long creation = data.getCreationDate().getTime() / 1000;
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

                    BanHammer.punishPlayer(punishment, true, true);

                    if (remove) {
                        banList.remove(data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            for (BannedIpEntry data : ipBanList.values()) {
                try {
                    String ip = ((ServerConfigEntryAccessor<String>) data).getKeyServer();

                    long creation = data.getCreationDate().getTime() / 1000;
                    long expiration;

                    try {
                        expiration = data.getExpiryDate().getTime() / 1000 - creation;
                    } catch (Exception e) {
                        expiration = -1;
                    }

                    BasicPunishment punishment = new BasicPunishment(
                            Util.NIL_UUID,
                            ip,
                            new LiteralText("Unknown player"),
                            "Unknown player",
                            Util.NIL_UUID,
                            new LiteralText(data.getSource()),
                            creation,
                            expiration,
                            data.getReason(),
                            PunishmentTypes.IPBAN);

                    BanHammer.punishPlayer(punishment, true, true);

                    if (remove) {
                        ipBanList.remove(data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}