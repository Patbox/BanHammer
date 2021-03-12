package eu.pb4.banhammer;

import eu.pb4.banhammer.types.BasicPunishment;
import net.kyori.adventure.text.minimessage.Template;
import net.minecraft.text.Text;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    public static Text parseMessage(String minimessage, List<Template> templates) {
        return BanHammerMod.getAdventure().toNative(BanHammerMod.miniMessage.parse(minimessage, templates)).shallowCopy();
    }

    public static List<Template> getTemplateFor(BasicPunishment punishment) {
        ArrayList<Template> list = new ArrayList<>();

        list.add(Template.of("operator", BanHammerMod.getAdventure().toAdventure(punishment.getNameOfAdmin().shallowCopy())));
        list.add(Template.of("reason", punishment.getReason()));
        list.add(Template.of("expiration_date", punishment.getFormattedExpirationDate()));
        list.add(Template.of("expiration_time", punishment.getFormattedExpirationTime()));
        list.add(Template.of("banned", BanHammerMod.getAdventure().toAdventure(punishment.getNameOfPlayer().shallowCopy())));

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
}





