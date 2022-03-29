package eu.pb4.banhammer.impl;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.net.SocketAddress;
import java.util.*;

public final class BHUtils {
    private static final Text UNKNOWN_PLAYER = new LiteralText("Unknown player").formatted(Formatting.ITALIC);

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

                switch (suffixOnly) {
                    case "c":
                        time += Double.parseDouble(numberOnly) * 3155692600L;
                        break;
                    case "y":
                        time += Double.parseDouble(numberOnly) * 31556926;
                        break;
                    case "mo":
                        time += Double.parseDouble(numberOnly) * 2592000;
                        break;
                    case "d":
                        time += Double.parseDouble(numberOnly) * 86400;
                        break;
                    case "h":
                        time += Double.parseDouble(numberOnly) * 3600;
                        break;
                    case "m":
                        time += Double.parseDouble(numberOnly) * 60;
                        break;
                    default:
                        time += Double.parseDouble(numberOnly);
                        break;
                }
            }
            return time;
        }
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


            ServerPlayerEntity player = isUuid
                    ? server.getPlayerManager().getPlayer(uuid)
                    : isIpLike
                    ? null
                    : server.getPlayerManager().getPlayer(usernameOrIp);

            if (player != null) {
                return List.of(new BHPlayerData(player.getUuid(), player.getGameProfile().getName(), player.getIp(), player.getDisplayName(), player));
            }

            if (isIpLike) {
                var uuids = BanHammerImpl.IP_TO_UUID_CACHE.get(usernameOrIp);
                if (uuids.isEmpty()) {
                    return Collections.emptyList();
                } else {
                    var list = new ArrayList<BHPlayerData>();

                    for (var uuid2 : uuids) {
                        var optional = server.getUserCache().getByUuid(uuid2);

                        if (optional.isPresent()) {
                            var profile = optional.get();
                            list.add(new BHPlayerData(profile.getId(), profile.getName(), usernameOrIp, new LiteralText(profile.getName()), server.getPlayerManager().getPlayer(profile.getId())));
                        } else {
                            list.add(new BHPlayerData(uuid2, "??: " + uuid2, usernameOrIp, new LiteralText("??: " + uuid2).formatted(Formatting.ITALIC), null));
                        }
                    }

                    return list;
                }
            }

            String ip = "unknown";
            GameProfile profile = null;

            if (isUuid) {
                ip = BanHammerImpl.UUID_TO_IP_CACHE.getOrDefault(uuid, "unknown");
                profile = server.getUserCache().getByUuid(uuid).orElse(null);
            } else {
                var possibleProfile = server.getUserCache().findByName(usernameOrIp);

                if (possibleProfile.isPresent()) {
                    profile = possibleProfile.get();
                    ip = BanHammerImpl.UUID_TO_IP_CACHE.getOrDefault(profile.getId(), "unknown");
                }
            }

            if (profile == null) {
                return List.of(new BHPlayerData(uuid, "Unknown player", ip, UNKNOWN_PLAYER, null));
            } else {
                return List.of(new BHPlayerData(profile.getId(), profile.getName(), ip, new LiteralText(profile.getName()), null));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static long getNow() {
        return System.currentTimeMillis() / 1000;
    }
}





