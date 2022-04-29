package eu.pb4.banhammer.impl.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.banhammer.api.PunishmentData;
import eu.pb4.banhammer.api.PunishmentType;
import eu.pb4.banhammer.impl.BHUtils;
import eu.pb4.banhammer.impl.BanHammerImpl;
import eu.pb4.banhammer.impl.config.Config;
import eu.pb4.banhammer.impl.config.ConfigManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PunishCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(create("kick", PunishmentType.KICK, false));

            dispatcher.register(create("mute", PunishmentType.MUTE, false));
            dispatcher.register(create("tempmute", PunishmentType.MUTE, true));

            dispatcher.register(create("ban", PunishmentType.BAN, false));
            dispatcher.register(create("tempban", PunishmentType.BAN, true));

            dispatcher.register(create("ban-ip", PunishmentType.IP_BAN, false));
            dispatcher.register(create("tempban-ip", PunishmentType.IP_BAN, true));

            dispatcher.register(create("warn", PunishmentType.WARN, false));
            dispatcher.register(create("tempwarn", PunishmentType.WARN, true));
        });
    }

    private static LiteralArgumentBuilder<ServerCommandSource> create(String command, PunishmentType type, boolean temp) {
        return literal(command)
                .requires(ConfigManager.requirePermissionOrOp("banhammer.punish." + command))
                .then(GeneralCommands.playerArgument("player")
                        .then(temp
                                ? argument("duration", StringArgumentType.word())
                                .executes(ctx -> punishCommand(ctx, true, type))
                                .then(argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> punishCommand(ctx, true, type))
                                )
                                : argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> punishCommand(ctx, false, type))
                        )
                );
    }

    private static int punishCommand(CommandContext<ServerCommandSource> ctx, boolean isTemp, PunishmentType type) {
        CompletableFuture.runAsync(() -> {
            Config config = ConfigManager.getConfig();
            String playerNameOrIp = ctx.getArgument("player", String.class);
            long duration = -1;

            if (isTemp) {
                try {
                    String durText = ctx.getArgument("duration", String.class);
                    long temp = BHUtils.parseDuration(durText);

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

            var players = BHUtils.lookupPlayerData(playerNameOrIp, ctx.getSource().getServer());

            if (players.isEmpty()) {
                ctx.getSource().sendFeedback(new LiteralText("Couldn't find player " + playerNameOrIp + "!").formatted(Formatting.RED), false);
            } else {
                for (var player : players) {
                    var punishment = PunishmentData.create(player.uuid(), player.ip(), player.displayName(), player.name(), ctx.getSource(), reason, duration, type);

                    BanHammerImpl.punishPlayer(punishment, config.configData.punishmentsAreSilent || isSilent);

                    if (config.configData.punishmentsAreSilent && !Permissions.check(ctx.getSource(), "banhammer.seesilent", 1)) {
                        ctx.getSource().sendFeedback(punishment.getChatMessage(), false);
                    }
                }
            }
        });
        return 1;
    }
}
