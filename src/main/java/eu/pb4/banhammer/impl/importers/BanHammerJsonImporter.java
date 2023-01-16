package eu.pb4.banhammer.impl.importers;

import com.google.gson.reflect.TypeToken;
import eu.pb4.banhammer.api.BanHammer;
import eu.pb4.banhammer.api.PunishmentData;
import eu.pb4.banhammer.api.PunishmentType;
import eu.pb4.banhammer.impl.BanHammerImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class BanHammerJsonImporter implements BanHammer.PunishmentImporter {
    public static final Path DEFAULT_PATH = FabricLoader.getInstance().getGameDir().resolve("banhammer_exports.json");

    public static String exportJson(boolean history) {
        return BanHammerImpl.GSON.toJson(export(history));
    }

    @Override
    public boolean importPunishments(MinecraftServer server, PunishmentConsumer consumer, boolean remove) {
        try {
            var obj = BanHammerImpl.GSON.fromJson(Files.readString(DEFAULT_PATH), Serialized.class);
            if (obj.active != null) {
                obj.active.forEach(consumer);
            }

            if (obj.history != null) {
                obj.history.forEach(consumer::acceptHistory);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Serialized export(boolean history) {
        var serialized = new Serialized();
        var punishments = new HashSet<PunishmentData>();
        serialized.active = punishments;
        for (var type : PunishmentType.values()) {
            if (type.useDatabase()) {
                BanHammerImpl.DATABASE.getAllPunishments(type, (p) -> {
                    if (!p.isExpired()) {
                        punishments.add(p);
                    }
                });
            }
        }


        if (history) {
            serialized.history = new ArrayList<>();
            BanHammerImpl.DATABASE.getAllPunishmentsHistory(serialized.history::add);
        }

        return serialized;
    }

    public static class Serialized {
        public Collection<PunishmentData> active;
        public Collection<PunishmentData> history;
    }
}