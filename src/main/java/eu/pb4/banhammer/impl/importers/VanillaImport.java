package eu.pb4.banhammer.impl.importers;

import com.mojang.authlib.GameProfile;
import eu.pb4.banhammer.api.BanHammer;
import eu.pb4.banhammer.impl.BanHammerImpl;
import eu.pb4.banhammer.mixin.accessor.ServerConfigEntryAccessor;
import eu.pb4.banhammer.api.PunishmentData;
import eu.pb4.banhammer.api.PunishmentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.*;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.util.Util;

public final class VanillaImport implements BanHammer.PunishmentImporter {
    public boolean importPunishments(MinecraftServer server, PunishmentConsumer consumer, boolean remove) {
        try {
            UserBanList banList = BanHammerImpl.SERVER.getPlayerList().getBans();
            IpBanList ipBanList = BanHammerImpl.SERVER.getPlayerList().getIpBans();

            for (UserBanListEntry data : banList.getEntries()) {
                try {
                    GameProfile profile = ((ServerConfigEntryAccessor<GameProfile>) data).getKeyServer();

                    long creation = data.getCreated().getTime() / 1000;
                    long expiration;

                    try {
                        expiration = data.getExpires().getTime() / 1000 - creation;
                    } catch (Exception e) {
                        expiration = -1;
                    }

                    PunishmentData punishment = new PunishmentData(
                            profile.id(),
                            "undefined",
                            Component.literal(profile.name()),
                            profile.name(),
                            Util.NIL_UUID,
                            Component.literal(data.getSource()),
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

            for (IpBanListEntry data : ipBanList.getEntries()) {
                try {
                    String ip = ((ServerConfigEntryAccessor<String>) data).getKeyServer();

                    long creation = data.getCreated().getTime() / 1000;
                    long expiration;

                    try {
                        expiration = data.getExpires().getTime() / 1000 - creation;
                    } catch (Exception e) {
                        expiration = -1;
                    }

                    PunishmentData punishment = new PunishmentData(
                            Util.NIL_UUID,
                            ip,
                            Component.literal("Unknown player"),
                            "Unknown player",
                            Util.NIL_UUID,
                            Component.literal(data.getSource()),
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