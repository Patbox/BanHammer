package eu.pb4.banhammer.impl.importers;

import com.mojang.authlib.GameProfile;
import eu.pb4.banhammer.api.BanHammer;
import eu.pb4.banhammer.impl.BanHammerImpl;
import eu.pb4.banhammer.mixin.accessor.ServerConfigEntryAccessor;
import eu.pb4.banhammer.api.PunishmentData;
import eu.pb4.banhammer.api.PunishmentType;
import net.minecraft.server.*;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public final class VanillaImport implements BanHammer.PunishmentImporter {
    public boolean importPunishments(MinecraftServer server, PunishmentConsumer consumer, boolean remove) {
        try {
            BannedPlayerList banList = BanHammerImpl.SERVER.getPlayerManager().getUserBanList();
            BannedIpList ipBanList = BanHammerImpl.SERVER.getPlayerManager().getIpBanList();

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

                    PunishmentData punishment = new PunishmentData(
                            profile.getId(),
                            "undefined",
                            Text.literal(profile.getName()),
                            profile.getName(),
                            Util.NIL_UUID,
                            Text.literal(data.getSource()),
                            creation,
                            expiration,
                            data.getReason(),
                            PunishmentType.BAN);

                    consumer.accept(punishment);

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

                    PunishmentData punishment = new PunishmentData(
                            Util.NIL_UUID,
                            ip,
                            Text.literal("Unknown player"),
                            "Unknown player",
                            Util.NIL_UUID,
                            Text.literal(data.getSource()),
                            creation,
                            expiration,
                            data.getReason(),
                            PunishmentType.IP_BAN);

                    consumer.accept(punishment);

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