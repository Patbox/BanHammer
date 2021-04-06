package eu.pb4.banhammer.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.banhammer.BanHammerMod;
import eu.pb4.banhammer.Helpers;
import eu.pb4.banhammer.config.Config;
import eu.pb4.banhammer.config.ConfigManager;
import eu.pb4.banhammer.types.BHPlayerData;
import eu.pb4.banhammer.types.BasicPunishment;
import eu.pb4.banhammer.types.PunishmentTypes;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;


import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PunishCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("kick")
                    .requires(ConfigManager.requirePermissionOrOp("banhammer.punish.kick"))
                    .then(playerArgument("player")
                            .executes(ctx -> punishCommand(ctx, false, PunishmentTypes.KICK))
                            .then(argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> punishCommand(ctx,false, PunishmentTypes.KICK))
                            )
                    ));

            dispatcher.register(literal("mute")
                    .requires(ConfigManager.requirePermissionOrOp("banhammer.punish.mute"))
                    .then(playerArgument("player")
                            .executes(ctx -> punishCommand(ctx, false, PunishmentTypes.MUTE))
                            .then(argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> punishCommand(ctx, false, PunishmentTypes.MUTE))
                            )
            ));

            dispatcher.register(literal("ban")
                    .requires(ConfigManager.requirePermissionOrOp("banhammer.punish.ban"))
                    .then(playerArgument("player")
                            .executes(ctx -> punishCommand(ctx, false, PunishmentTypes.BAN))
                            .then(argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> punishCommand(ctx,false, PunishmentTypes.BAN))
                            )
            ));

            dispatcher.register(literal("ban-ip")
                    .requires(ConfigManager.requirePermissionOrOp("banhammer.punish.ban-ip"))
                    .then(playerArgument("player")
                            .executes(ctx -> punishCommand(ctx, false, PunishmentTypes.IPBAN))
                            .then(argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> punishCommand(ctx,false, PunishmentTypes.IPBAN))
                            )
            ));

            dispatcher.register(literal("tempmute")
                    .requires(ConfigManager.requirePermissionOrOp("banhammer.punish.tempmute"))
                    .then(playerArgument("player")
                            .then(argument("duration", StringArgumentType.word())
                                    .executes(ctx -> punishCommand(ctx, true, PunishmentTypes.MUTE))
                                    .then(argument("reason", StringArgumentType.greedyString())
                                            .executes(ctx -> punishCommand(ctx, true, PunishmentTypes.MUTE))
                            ))
                    ));

            dispatcher.register(literal("tempban")
                    .requires(ConfigManager.requirePermissionOrOp("banhammer.punish.tempban"))
                    .then(playerArgument("player")
                            .then(argument("duration", StringArgumentType.word())
                                    .executes(ctx -> punishCommand(ctx,true, PunishmentTypes.BAN))
                                    .then(argument("reason", StringArgumentType.greedyString())
                                            .executes(ctx -> punishCommand(ctx,true, PunishmentTypes.BAN))
                                    )
                            )
                    ));

            dispatcher.register(literal("tempban-ip")
                    .requires(ConfigManager.requirePermissionOrOp("banhammer.punish.tempban-ip"))
                    .then(playerArgument("player")
                            .then(argument("duration", StringArgumentType.word())
                                    .executes(ctx -> punishCommand(ctx, true, PunishmentTypes.IPBAN))
                                    .then(argument("reason", StringArgumentType.greedyString())
                                            .executes(ctx -> punishCommand(ctx,true, PunishmentTypes.IPBAN))
                            ))
                    ));
        });
    }

    private static int punishCommand(CommandContext<ServerCommandSource> ctx, boolean isTemp, PunishmentTypes type) {
        CompletableFuture.runAsync(() -> {
            Config config = ConfigManager.getConfig();
            String playerNameOrIp = ctx.getArgument("player", String.class);
            long duration = -1;

            if (isTemp) {
                try {
                    String durText = ctx.getArgument("duration", String.class);
                    long temp = Helpers.parseDuration(durText);

                    if (Permissions.check(ctx.getSource(), "banhammer.ignoreTempLimit", 2)) {
                        duration = temp;
                    } else {
                        long temp2 = ConfigManager.getConfig().getDurationLimit(ctx.getSource());

                        duration = Math.min(temp2, temp);
                    }

                } catch (Exception e) {
                    ctx.getSource().sendError(new LiteralText("Invalid duration!"));
                    return;
                }
            }


            String reason;
            boolean isSilent;

            try {
                String temp = ctx.getArgument("reason", String.class);
                if (temp.startsWith("-")) {
                    String[] parts = temp.split(" ", 2);

                    isSilent = parts[0].contains("s");
                    reason = parts.length == 2 ? parts[1] : config.defaultReason;
                } else {
                    reason = temp;
                    isSilent = false;
                }

            } catch (Exception e) {
                reason = config.defaultReason;
                isSilent = false;
            }

            BHPlayerData player = Helpers.lookupPlayerData(playerNameOrIp, type);

            if (player == null) {
                ctx.getSource().sendFeedback(new LiteralText("Couldn't find player " + playerNameOrIp + "!").formatted(Formatting.RED), false);
            }

            UUID playerUUID = player.uuid;
            Text playerDisplay = player.displayName;
            String playerName = player.name;
            String playerIP = player.ip;


            ServerPlayerEntity executor;
            UUID executorUUID;
            try {
                executor = ctx.getSource().getPlayer();
                executorUUID = executor.getUuid();
            } catch (Exception e) {
                executorUUID = Util.NIL_UUID;
            }

            BasicPunishment punishment = new BasicPunishment(playerUUID, playerIP, playerDisplay, playerName, executorUUID, ctx.getSource().getDisplayName(), System.currentTimeMillis() / 1000, duration, reason, type);

            BanHammerMod.punishPlayer(punishment, config.configData.punishmentsAreSilent || isSilent);

            if (config.configData.punishmentsAreSilent && !Permissions.check(ctx.getSource(), "banhammer.seesilent", 1)) {
                ctx.getSource().sendFeedback(punishment.getChatMessage(), false);
            }
        });
        return 1;
    }


    public static RequiredArgumentBuilder<ServerCommandSource, String> playerArgument(String name) {
        return CommandManager.argument(name, StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

                    for (String player : ctx.getSource().getMinecraftServer().getPlayerNames()) {
                        if (player.toLowerCase(Locale.ROOT).contains(remaining)) {
                            builder.suggest(player);
                        }
                    }

                    return builder.buildFuture();
                });
    }
}
