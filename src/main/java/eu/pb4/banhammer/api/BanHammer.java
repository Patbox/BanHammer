package eu.pb4.banhammer.api;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import eu.pb4.banhammer.impl.BanHammerImpl;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

@SuppressWarnings({ "unused" })
public final class BanHammer {
    private BanHammer() {
    }

    public static void punish(PunishmentData punishment) {
        punish(punishment, false, false);
    }

    public static void punish(PunishmentData punishment, boolean silent) {
        punish(punishment, silent, false);
    }

    public static void punish(PunishmentData punishment, boolean silent, boolean invisible) {
        BanHammerImpl.punishPlayer(punishment, silent, invisible);
    }

    public static int removePunishment(PunishmentData.Synced punishment) {
        return BanHammerImpl.removePunishment(punishment);
    }

    public static int removePunishment(UUID uuid, PunishmentType type) {
        return BanHammerImpl.removePunishment(uuid.toString(), type);
    }

    public static int removePunishment(String ip, PunishmentType type) {
        if (InetAddresses.isInetAddress(ip)) {
            return BanHammerImpl.removePunishment(ip, type);
        }
        return 0;
    }

    public static Collection<PunishmentData.Synced> getPunishments(String ip, PunishmentType type) {
        if (InetAddresses.isInetAddress(ip)) {
            return BanHammerImpl.getPlayersPunishments(ip, type);
        }
        return Collections.emptyList();
    }

    public static Collection<PunishmentData.Synced> getPunishments(UUID uuid, PunishmentType type) {
        return BanHammerImpl.getPlayersPunishments(uuid.toString(), type);
    }

    public static Collection<PunishmentData.Synced> getPunishments(String ip) {
        var list = new ArrayList<PunishmentData.Synced>();
        if (InetAddresses.isInetAddress(ip)) {
            for (var type : PunishmentType.values()) {
                list.addAll(BanHammerImpl.getPlayersPunishments(ip, type));
            }
        }

        return list;
    }

    public static Collection<PunishmentData.Synced> getPunishments(UUID uuid) {
        var list = new ArrayList<PunishmentData.Synced>();
        var id = uuid.toString();
        for (var type : PunishmentType.values()) {
            list.addAll(BanHammerImpl.getPlayersPunishments(id, type));
        }

        return list;
    }

    public static Collection<PunishmentData.Synced> getAllPunishments(PunishmentType type) {
        var list = new ArrayList<PunishmentData.Synced>();
        BanHammerImpl.DATABASE.getAllPunishments(type, list::add);
        return list;
    }

    public static Collection<PunishmentData.Synced> getAllPunishments() {
        var list = new ArrayList<PunishmentData.Synced>();
        for (var type : PunishmentType.values()) {
            BanHammerImpl.DATABASE.getAllPunishments(type, list::add);
        }
        return list;
    }

    public static boolean isPunished(UUID uuid, PunishmentType type) {
        return BanHammerImpl.isPlayerPunished(uuid.toString(), type);
    }

    public static boolean isPunished(String ip, PunishmentType type) {
        if (InetAddresses.isInetAddress(ip)) {
            return BanHammerImpl.isPlayerPunished(ip, type);
        }
        return false;
    }

    public static void registerPunishmentEvent(PunishmentEvent event) {
        BanHammerImpl.PUNISHMENT_EVENT.register(event);
    }

    public static void registerPunishmentCheckEvent(PunishmentCheckEvent event) {
        BanHammerImpl.CAN_PUNISH_CHECK_EVENT.register(event);
    }

    public static void registerImporter(Identifier identifier, PunishmentImporter importer) {
        BanHammerImpl.IMPORTERS.put(identifier.toString(), importer);
    }

    @FunctionalInterface
    public interface PunishmentEvent {
        void onPunishment(PunishmentData punishment, boolean silent, boolean invisible);
    }

    @FunctionalInterface
    public interface PunishmentCheckEvent {
        TriState canSourcePunish(GameProfile profile, ServerCommandSource source);
    }

    public interface PunishmentImporter {
        @Deprecated
        default boolean importPunishments(MinecraftServer server, Consumer<PunishmentData> consumer, boolean remove) {
            return false;
        }

        default boolean importPunishments(MinecraftServer server, PunishmentConsumer consumer, boolean remove) {
            return importPunishments(server, consumer, remove);
        }

        interface PunishmentConsumer extends Consumer<PunishmentData> {
            static PunishmentConsumer of(Consumer<PunishmentData> active, Consumer<PunishmentData> history) {
                return new PunishmentConsumer() {
                    @Override
                    public void accept(PunishmentData data) {
                        active.accept(data);
                    }

                    @Override
                    public void acceptHistory(PunishmentData data) {
                        history.accept(data);
                    }
                };
            }

            void accept(PunishmentData data);
            void acceptHistory(PunishmentData data);
        }
    }


}
