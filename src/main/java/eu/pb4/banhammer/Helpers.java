package eu.pb4.banhammer;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import eu.pb4.banhammer.types.BHPlayerData;
import eu.pb4.banhammer.types.BasicPunishment;
import eu.pb4.banhammer.types.PunishmentTypes;
import net.kyori.adventure.text.minimessage.Template;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

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

    public static Text parseMessage(String minimessage) {
        return BanHammerMod.getAdventure().toNative(BanHammerMod.miniMessage.parse(minimessage)).shallowCopy();
    }

    public static Text parseMessage(String minimessage, List<Template> templates) {
        return BanHammerMod.getAdventure().toNative(BanHammerMod.miniMessage.parse(minimessage, templates)).shallowCopy();
    }

    public static List<Template> getTemplateFor(BasicPunishment punishment) {
        ArrayList<Template> list = new ArrayList<>();

        list.add(Template.of("operator", BanHammerMod.getAdventure().toAdventure(punishment.adminDisplayName.shallowCopy())));
        list.add(Template.of("reason", punishment.reason));
        list.add(Template.of("expiration_date", punishment.getFormattedExpirationDate()));
        list.add(Template.of("expiration_time", punishment.getFormattedExpirationTime()));
        list.add(Template.of("banned", BanHammerMod.getAdventure().toAdventure(punishment.bannedDisplayName.shallowCopy())));

        return list;
    }

    public static long parseDuration(String text) throws NumberFormatException {
        text = text.toLowerCase(Locale.ROOT);
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            String[] times = text.replaceAll("([a-z]+)", "$1|").split("\\|");
            long time = 0;
            for (String x : times) {
                String numberOnly = x.replaceAll("([a-z])", "");
                String suffixOnly = x.replaceAll("([^a-z])", "");

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

    public static BHPlayerData lookupPlayerData(String usernameOrIp, PunishmentTypes type) {
        ServerPlayerEntity player = BanHammerMod.SERVER.getPlayerManager().getPlayer(usernameOrIp);

        String name;
        Text displayName;
        UUID uuid = null;
        String ip = null;


        if (player != null) {
            uuid = player.getUuid();
            ip = player.getIp();
            displayName = player.getDisplayName();
            name = player.getGameProfile().getName();
        } else if (type.ipBased && InetAddresses.isInetAddress(usernameOrIp)) {
            GameProfile profile = null;

            for (Map.Entry<String, String> entry : BanHammerMod.IP_CACHE.entrySet()) {
                if (entry.getValue().equals(usernameOrIp)) {
                    uuid = UUID.fromString(entry.getKey());
                    ip = entry.getValue();
                    profile = BanHammerMod.SERVER.getUserCache().getByUuid(uuid);
                    break;
                }
            }

            if (profile == null) {
                name = "Unknown player";
                displayName = new LiteralText("Unknown player").formatted(Formatting.ITALIC);
                uuid = Util.NIL_UUID;
            } else {
                displayName = new LiteralText(profile.getName());
                name = profile.getName();
            }
        } else {
            GameProfile profile = BanHammerMod.SERVER.getUserCache().findByName(usernameOrIp);

            if (profile != null) {
                uuid = profile.getId();
                ip = BanHammerMod.IP_CACHE.get(uuid.toString());
                displayName = new LiteralText(profile.getName());
                name = profile.getName();
            } else {
                return null;
            }
        }
        return new BHPlayerData(uuid, name, ip, displayName, player);
    }
}





