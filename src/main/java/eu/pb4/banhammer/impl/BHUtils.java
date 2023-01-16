package eu.pb4.banhammer.impl;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import eu.pb4.banhammer.impl.config.ConfigManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.SocketAddress;
import java.util.*;

public final class BHUtils {
    private static final Text UNKNOWN_PLAYER = Text.literal("Unknown player").formatted(Formatting.ITALIC);

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

    public static boolean isPunishableBy(GameProfile profile, ServerCommandSource source) {
        var server = source.getServer();
        var entry = server.getPlayerManager().getOpList().get(profile);
        return (server.getName().equals("Server") && source.getEntity() == null) || ((entry == null || source.hasPermissionLevel(entry.getPermissionLevel()))
                && ConfigManager.getConfig().canPunish(profile)
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


            ServerPlayerEntity player = isUuid
                    ? server.getPlayerManager().getPlayer(uuid)
                    : isIpLike
                    ? null
                    : server.getPlayerManager().getPlayer(usernameOrIp);

            if (player != null) {
                return List.of(new BHPlayerData(player.getGameProfile(), player.getIp(), player.getDisplayName(), player));
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
                            list.add(new BHPlayerData(profile, usernameOrIp, Text.literal(profile.getName()), server.getPlayerManager().getPlayer(profile.getId())));
                        } else {
                            list.add(new BHPlayerData(new GameProfile(uuid2, null), usernameOrIp, Text.literal("??: " + uuid2).formatted(Formatting.ITALIC), null));
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
                return List.of(new BHPlayerData(new GameProfile(uuid, null), ip, UNKNOWN_PLAYER, null));
            } else {
                return List.of(new BHPlayerData(profile, ip, Text.literal(profile.getName()), null));
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





