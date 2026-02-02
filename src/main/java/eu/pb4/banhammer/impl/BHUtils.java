package eu.pb4.banhammer.impl;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import eu.pb4.banhammer.impl.config.ConfigManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.Util;

import java.net.SocketAddress;
import java.util.*;

public final class BHUtils {
    private static final Component UNKNOWN_PLAYER = Component.literal("Unknown player").withStyle(ChatFormatting.ITALIC);

    public static String stringifyAddress(SocketAddress socketAddress) {
        String string = socketAddress.toString();
        if (string.contains("/")) {
            string = string.substring(string.indexOf(47) + 1);
        }

        if (string.contains(":")) {
            string = string.substring(0, string.indexOf(58));
        }

        return string;
    }

    public static long parseDuration(String text) throws NumberFormatException {
        text = text.toLowerCase(Locale.ROOT);
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            String[] times = text.replaceAll("([a-z]+)", "$1|").split("\\|");
            long time = 0;
            for (String x : times) {
                String numberOnly = x.replaceAll("[a-z]", "");
                String suffixOnly = x.replaceAll("[^a-z]", "");

                time += switch (suffixOnly) {
                    case "c" -> Double.parseDouble(numberOnly) * 60 * 60 * 24L * 365L * 100L;
                    case "y", "year", "years" -> Double.parseDouble(numberOnly) * 60 * 60 * 24L * 365L;
                    case "mo", "month", "months" -> Double.parseDouble(numberOnly) * 60 * 60 * 24L * 30L;
                    case "w", "week", "weeks" -> Double.parseDouble(numberOnly) * 60 * 60 * 24L * 7L;
                    case "d", "day", "days" -> Double.parseDouble(numberOnly) * 60 * 60 * 24;
                    case "h", "hour", "hours" -> Double.parseDouble(numberOnly) * 60 * 60;
                    case "m", "minute", "minutes" -> Double.parseDouble(numberOnly) * 60;
                    default -> Double.parseDouble(numberOnly);
                };
            }
            return time;
        }
    }

    public static boolean isPunishableBy(GameProfile profile, CommandSourceStack source) {
        if (profile == null) {
            return true;
        }
        var server = source.getServer();
        var entry = server.getPlayerList().getOps().get(new NameAndId(profile));

        boolean permission = true;

        try {
            permission = Permissions.check(source, "banhammer.can_ban_admins") || !Permissions.check(profile, "banhammer.block_punishments").get();
        } catch (Throwable e) {
            e.printStackTrace();
        }


        return (server.name().equals("Server") && source.getEntity() == null) || ((entry == null || source.permissions().hasPermission(new Permission.HasCommandLevel(entry.permissions().level())))
                && ConfigManager.getConfig().canPunish(profile)
                && permission
                && BanHammerImpl.CAN_PUNISH_CHECK_EVENT.invoker().canSourcePunish(profile, source).get());
    }

    public static Collection<BHPlayerData> lookupPlayerData(String usernameOrIp, MinecraftServer server) {
        try {
            boolean isUuid;
            boolean isIpLike = InetAddresses.isInetAddress(usernameOrIp);

            UUID uuid = null;

            try {
                uuid = UUID.fromString(usernameOrIp);
                isUuid = true;
            } catch (Exception e) {
                isUuid = false;
            }


            ServerPlayer player = isUuid
                    ? server.getPlayerList().getPlayer(uuid)
                    : isIpLike
                    ? null
                    : server.getPlayerList().getPlayerByName(usernameOrIp);

            if (player != null) {
                return List.of(new BHPlayerData(player.getGameProfile(), player.getIpAddress(), player.getDisplayName(), player));
            }

            if (isIpLike) {
                var uuids = BanHammerImpl.IP_TO_UUID_CACHE.get(usernameOrIp);
                if (uuids == null || uuids.isEmpty()) {
                    return List.of(new BHPlayerData(null, usernameOrIp, Component.literal("??: <UNKNOWN>"), null));
                } else {
                    var list = new ArrayList<BHPlayerData>();

                    for (var uuid2 : uuids) {
                        var optional = server.services().nameToIdCache().get(uuid2);

                        if (optional.isPresent()) {
                            var profile = optional.get();
                            list.add(new BHPlayerData(new GameProfile(profile.id(), profile.name()), usernameOrIp, Component.literal(profile.name()), server.getPlayerList().getPlayer(profile.id())));
                        } else {
                            list.add(new BHPlayerData(new GameProfile(uuid2, null), usernameOrIp, Component.literal("??: " + uuid2).withStyle(ChatFormatting.ITALIC), null));
                        }
                    }

                    return list;
                }
            }

            String ip = "unknown";
            GameProfile profile = null;

            if (isUuid) {
                ip = BanHammerImpl.UUID_TO_IP_CACHE.getOrDefault(uuid, "unknown");
                var tmp = server.services().nameToIdCache().get(uuid).orElse(null);
                profile = tmp != null ? new GameProfile(tmp.id(), tmp.name()) : null;
            } else {
                var possibleProfile = server.services().nameToIdCache().get(usernameOrIp);

                if (possibleProfile.isPresent()) {
                    profile = new GameProfile(possibleProfile.get().id(), possibleProfile.get().name(), PropertyMap.EMPTY);
                    ip = BanHammerImpl.UUID_TO_IP_CACHE.getOrDefault(profile.id(), "unknown");
                }
            }

            if (profile == null) {
                return List.of(new BHPlayerData(new GameProfile(uuid, ""), ip, UNKNOWN_PLAYER, null));
            } else {
                return List.of(new BHPlayerData(profile, ip, Component.literal(profile.name()), null));
            }
        } catch (Exception e) {
            //e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static long getNow() {
        return System.currentTimeMillis() / 1000;
    }
}





