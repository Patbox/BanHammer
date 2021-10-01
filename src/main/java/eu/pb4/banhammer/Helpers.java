package eu.pb4.banhammer;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import eu.pb4.banhammer.types.BHPlayerData;
import eu.pb4.banhammer.types.PunishmentTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.net.SocketAddress;
import java.util.*;

public class Helpers {
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

    public static @Nullable BHPlayerData lookupPlayerData(String usernameOrIp) {
        boolean isUuid;
        boolean isIpLike = InetAddresses.isInetAddress(usernameOrIp);

        String name;
        Text displayName;
        UUID uuid = null;
        String ip = null;

        try {
            uuid = UUID.fromString(usernameOrIp);
            isUuid = true;
        } catch (Exception e) {
            isUuid = false;
        }



        ServerPlayerEntity player = isUuid
                ? BanHammer.SERVER.getPlayerManager().getPlayer(uuid)
                : isIpLike
                ? null
                : BanHammer.SERVER.getPlayerManager().getPlayer(usernameOrIp);


        if (player != null) {
            uuid = player.getUuid();
            ip = player.getIp();
            displayName = player.getDisplayName();
            name = player.getGameProfile().getName();
        } else {
            GameProfile profile = null;

            if (isUuid) {
                ip = BanHammer.UUID_TO_IP_CACHE.get(uuid);
            } else if (isIpLike) {
                uuid = BanHammer.IP_TO_UUID_CACHE.get(usernameOrIp);
                ip = usernameOrIp;
            } else {
                var possibleProfile = BanHammer.SERVER.getUserCache().findByName(usernameOrIp);

                if (possibleProfile.isPresent()) {
                    profile = possibleProfile.get();
                    uuid = profile.getId();
                }
            }

            if (uuid != null && profile == null) {
                profile = BanHammer.SERVER.getUserCache().getByUuid(uuid).orElse(null);
            }

            if (profile == null) {
                name = "Unknown player";
                displayName = new LiteralText("Unknown player").formatted(Formatting.ITALIC);
                uuid = Util.NIL_UUID;
            } else {
                displayName = new LiteralText(profile.getName());
                name = profile.getName();
                ip = BanHammer.UUID_TO_IP_CACHE.get(uuid);
            }
        }

        return new BHPlayerData(uuid, name, ip, displayName, player);
    }
}





