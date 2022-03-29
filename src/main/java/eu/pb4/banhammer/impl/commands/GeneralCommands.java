package eu.pb4.banhammer.impl.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.banhammer.api.PunishmentData;
import eu.pb4.banhammer.impl.BHUtils;
import eu.pb4.banhammer.impl.BanHammerImpl;
import eu.pb4.banhammer.impl.GenericModInfo;
import eu.pb4.banhammer.impl.config.ConfigManager;
import eu.pb4.sgui.api.elements.BookElementBuilder;
import eu.pb4.sgui.api.gui.BookGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

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
                                            .executes((ctx) -> GeneralCommands.importer(ctx, false))
                                            .then(literal("remove")
                                                    .executes((ctx) -> GeneralCommands.importer(ctx, true))
                                            )
                                    )
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
                ctx.getSource().sendFeedback(new LiteralText("Player not found!").formatted(Formatting.RED), false);
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
                        new LiteralText("User: ").setStyle(Style.EMPTY.withBold(true)).append(new LiteralText(p.playerName).setStyle(Style.EMPTY.withBold(false))),
                        new LiteralText("Type: ").setStyle(Style.EMPTY.withBold(true)).append(new LiteralText(p.type.name).setStyle(Style.EMPTY.withBold(false))),
                        new LiteralText("Date: ").setStyle(Style.EMPTY.withBold(true)).append(new LiteralText(p.getFormattedDate()).setStyle(Style.EMPTY.withBold(false))),
                        new LiteralText("Expires: ").setStyle(Style.EMPTY.withBold(true)).append(new LiteralText(p.getFormattedExpirationDate()).setStyle(Style.EMPTY.withBold(false))),
                        new LiteralText("By: ").setStyle(Style.EMPTY.withBold(true)).append(p.adminDisplayName.shallowCopy().setStyle(Style.EMPTY.withBold(p.adminDisplayName.getStyle().isBold() == true))),
                        new LiteralText("Reason: ").setStyle(Style.EMPTY.withBold(true)).append(new LiteralText(p.reason).setStyle(Style.EMPTY.withBold(false)))
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
            context.getSource().sendFeedback(new LiteralText("Reloaded config!"), false);
        } else {
            context.getSource().sendError(new LiteralText("Error accrued while reloading config!").formatted(Formatting.RED));
        }
        return 1;
    }

    private static int about(CommandContext<ServerCommandSource> context) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            GenericModInfo.build(FabricLoader.getInstance().getModContainer("banhammer").get());
        }

        for (var text : context.getSource().getEntity() instanceof ServerPlayerEntity ? GenericModInfo.getAboutFull() : GenericModInfo.getAboutConsole()) {
            context.getSource().sendFeedback(text, false);
        }
        return 1;
    }

    private static int importer(CommandContext<ServerCommandSource> context, boolean remove) {
        String type = context.getArgument("source", String.class);

        var importer = BanHammerImpl.IMPORTERS.get(type);

        if (importer != null) {
            boolean result = importer.importPunishments(context.getSource().getServer(), (punishment) -> BanHammerImpl.punishPlayer(punishment, true, true), remove);

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
