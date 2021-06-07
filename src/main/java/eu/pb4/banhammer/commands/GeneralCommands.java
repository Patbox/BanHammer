package eu.pb4.banhammer.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.banhammer.BanHammerMod;
import eu.pb4.banhammer.Helpers;
import eu.pb4.banhammer.config.ConfigManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import java.util.Locale;

import static net.minecraft.server.command.CommandManager.literal;

public class GeneralCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(
                    literal("banhammer")
                            .requires(Permissions.require("banhammer.commands.main", true))
                            .executes(GeneralCommands::about)
                            .then(literal("reload")
                                    .requires(Permissions.require("banhammer.commands.reload", 4))
                                    .executes(GeneralCommands::reloadConfig)
                            )
                            .then(literal("import")
                                    .requires(Permissions.require("banhammer.commands.import", 4))
                                    .then(importArgument("source")
                                            .executes(GeneralCommands::importer)
                                            .then(CommandManager.argument("remove", BoolArgumentType.bool())
                                                    .executes(GeneralCommands::importer)
                                            )
                                    )
                            )
                );
            });
        }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        if (ConfigManager.loadConfig()) {
            context.getSource().sendFeedback(new LiteralText("Reloaded config!"), false);
        } else {
            context.getSource().sendError(new LiteralText("Error accrued while reloading config!").formatted(Formatting.RED));
        }
        return 1;
    }

    private static int about(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(new LiteralText("BanHammer").formatted(Formatting.RED)
                        .append(new LiteralText(" - " + BanHammerMod.VERSION).formatted(Formatting.WHITE)), false);
        return 1;
    }

    private static int importer(CommandContext<ServerCommandSource> context) {
        String type = context.getArgument("source", String.class);
        boolean remove;
        try {
            remove = context.getArgument("remove", Boolean.class);
        } catch (Exception e) {
            remove = false;
        }

        BanHammerMod.PunishmentImporter importer = BanHammerMod.IMPORTERS.get(type);

        if (importer != null) {
            boolean result = importer.importPunishments(remove);

            if (result) {
                context.getSource().sendFeedback(new LiteralText("Successfully imported punishments!").formatted(Formatting.GREEN), false);
                return 1;
            } else {
                context.getSource().sendError(new LiteralText("Couldn't import punishments!"));
                return 0;
            }
        } else {
            context.getSource().sendError(new LiteralText("Invalid importer type!"));
            return 0;
        }

    }



    public static RequiredArgumentBuilder<ServerCommandSource, String> importArgument(String name) {
        return CommandManager.argument(name, StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

                    for (String type : BanHammerMod.IMPORTERS.keySet()) {
                        if (type.contains(remaining)) {
                            builder.suggest(type);
                        }
                    }

                    return builder.buildFuture();
                });
    }
}
