package eu.pb4.banhammer.impl.commands;

import com.google.gson.Gson;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.banhammer.api.BanHammer;
import eu.pb4.banhammer.api.PunishmentData;
import eu.pb4.banhammer.api.PunishmentType;
import eu.pb4.banhammer.impl.BHUtils;
import eu.pb4.banhammer.impl.BanHammerImpl;
import eu.pb4.banhammer.impl.GenericModInfo;
import eu.pb4.banhammer.impl.config.ConfigManager;
import eu.pb4.banhammer.impl.importers.BanHammerJsonImporter;
import eu.pb4.sgui.api.elements.BookElementBuilder;
import eu.pb4.sgui.api.gui.BookGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.literal;

public class GeneralCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
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
                                            .executes((ctx) -> GeneralCommands.importer(ctx, false))
                                            .then(literal("remove")
                                                    .executes((ctx) -> GeneralCommands.importer(ctx, true))
                                            )
                                    )
                            )
                            .then(literal("export_all_punishments")
                                    .requires(Permissions.require("banhammer.commands.export", 4))
                                    .executes((ctx) -> GeneralCommands.exporter(ctx, false))
                                    .then(literal("with_history").executes((ctx) -> GeneralCommands.exporter(ctx, true)))
                            )
                            .then(literal("list")
                                    .requires(Permissions.require("banhammer.commands.list", 4))
                                    .then(playerArgument("player")
                                            .executes(GeneralCommands::listBans)
                                    )


                            )
            );
        });
    }

    private static int listBans(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var mainPlayer = ctx.getSource().getPlayer();
        CompletableFuture.runAsync(() -> {
            var punishments = new ArrayList<PunishmentData>();
            String playerNameOrIp = ctx.getArgument("player", String.class);
            var players = BHUtils.lookupPlayerData(playerNameOrIp, ctx.getSource().getServer());

            if (players.isEmpty()) {
                ctx.getSource().sendFeedback(() -> Text.literal("Player not found!").formatted(Formatting.RED), false);
                return;
            }

            for (var player : players) {
                if (ConfigManager.getConfig().configData.storeAllPunishmentsInHistory) {
                    BanHammerImpl.DATABASE.getPunishmentsHistory(player.uuid().toString(), punishments::add);
                } else {
                    punishments.addAll(BanHammerImpl.getPlayersPunishments(player.uuid().toString(), null));
                }
            }

            punishments.sort(Comparator.comparingLong(e -> -e.time));

            var book = new BookElementBuilder();

            for (var p : punishments) {
                book.addPage(
                        Text.literal("User: ").setStyle(Style.EMPTY.withBold(true)).append(Text.literal(p.playerName).setStyle(Style.EMPTY.withBold(false))),
                        Text.literal("Type: ").setStyle(Style.EMPTY.withBold(true)).append(Text.literal(p.type.name).setStyle(Style.EMPTY.withBold(false))),
                        Text.literal("Date: ").setStyle(Style.EMPTY.withBold(true)).append(Text.literal(p.getFormattedDate()).setStyle(Style.EMPTY.withBold(false))),
                        Text.literal("Expires: ").setStyle(Style.EMPTY.withBold(true)).append(Text.literal(p.getFormattedExpirationDate()).setStyle(Style.EMPTY.withBold(false))),
                        Text.literal("By: ").setStyle(Style.EMPTY.withBold(true)).append(p.adminDisplayName.copy().setStyle(Style.EMPTY.withBold(p.adminDisplayName.getStyle().isBold() == true))),
                        Text.literal("Reason: ").setStyle(Style.EMPTY.withBold(true)).append(Text.literal(p.reason).setStyle(Style.EMPTY.withBold(false)))
                );
            }

            ctx.getSource().getServer().execute(() -> {
                try {
                    new BookGui(mainPlayer, book).open();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        });
        return 0;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        if (ConfigManager.loadConfig()) {
            context.getSource().sendFeedback(() -> Text.literal("Reloaded config!"), false);
        } else {
            context.getSource().sendError(Text.literal("Error accrued while reloading config!").formatted(Formatting.RED));
        }
        return 1;
    }

    private static int about(CommandContext<ServerCommandSource> context) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            GenericModInfo.build(FabricLoader.getInstance().getModContainer("banhammer").get());
        }

        for (var text : context.getSource().getEntity() instanceof ServerPlayerEntity ? GenericModInfo.getAboutFull() : GenericModInfo.getAboutConsole()) {
            context.getSource().sendFeedback(() -> text, false);
        }
        return 1;
    }

    private static int importer(CommandContext<ServerCommandSource> context, boolean remove) {
        String type = context.getArgument("source", String.class);

        var importer = BanHammerImpl.IMPORTERS.get(type);

        if (importer != null) {
            var history = new ArrayList<PunishmentData>();
            var active = new ArrayList<PunishmentData>();

            boolean result = importer.importPunishments(context.getSource().getServer(), BanHammer.PunishmentImporter.PunishmentConsumer.of(active::add, history::add), remove);

            for (var p : active) {
                BanHammerImpl.punishPlayer(p, true, true);
            }

            if (ConfigManager.getConfig().configData.storeAllPunishmentsInHistory && !history.isEmpty()) {
                CompletableFuture.runAsync(() -> {
                    for (var p : history) {
                        BanHammerImpl.DATABASE.insertPunishmentIntoHistory(p);
                    }
                });
            }

            if (result) {
                context.getSource().sendFeedback(() -> Text.literal("Successfully imported punishments!").formatted(Formatting.GREEN), false);
                return 1;
            } else {
                context.getSource().sendError(Text.literal("Couldn't import punishments!"));
                return 0;
            }
        } else {
            context.getSource().sendError(Text.literal("Invalid importer type!"));
            return 0;
        }
    }

    private static int exporter(CommandContext<ServerCommandSource> context, boolean history) {
        CompletableFuture.runAsync(() -> {
            try {
                Files.writeString(BanHammerJsonImporter.DEFAULT_PATH, BanHammerJsonImporter.exportJson(history));
                context.getSource().sendFeedback(() -> Text.literal("Successfully exported punishments to banhammer_exports.json file!").formatted(Formatting.GREEN), false);

            } catch (Throwable e) {
                context.getSource().sendError(Text.literal("Couldn't export punishments!"));
                e.printStackTrace();
            }
        });
        return 0 ;
    }


    public static RequiredArgumentBuilder<ServerCommandSource, String> importArgument(String name) {
        return CommandManager.argument(name, StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

                    for (String type : BanHammerImpl.IMPORTERS.keySet()) {
                        if (type.contains(remaining)) {
                            builder.suggest(type);
                        }
                    }

                    return builder.buildFuture();
                });
    }

    public static RequiredArgumentBuilder<ServerCommandSource, String> playerArgument(String name) {
        return CommandManager.argument(name, StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

                    for (String player : ctx.getSource().getServer().getPlayerNames()) {
                        if (player.toLowerCase(Locale.ROOT).contains(remaining)) {
                            builder.suggest(player);
                        }
                    }

                    return builder.buildFuture();
                });
    }
}
