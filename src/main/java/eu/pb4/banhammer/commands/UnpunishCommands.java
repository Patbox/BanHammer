package eu.pb4.banhammer.commands;


import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.banhammer.BanHammer;
import eu.pb4.banhammer.Helpers;
import eu.pb4.banhammer.config.Config;
import eu.pb4.banhammer.config.ConfigManager;
import eu.pb4.banhammer.config.data.DiscordMessageData;
import eu.pb4.banhammer.types.BHPlayerData;
import eu.pb4.banhammer.types.PunishmentTypes;
import eu.pb4.placeholders.PlaceholderAPI;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class UnpunishCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("unban")
                    .requires(ConfigManager.requirePermissionOrOp("banhammer.unpunish.unban"))
                    .then(playerArgument("player")
                            .executes(ctx -> removePunishmentCommand(ctx, PunishmentTypes.BAN))
                            .then(argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> removePunishmentCommand(ctx, PunishmentTypes.BAN))
                            )
                    ));

            dispatcher.register(literal("unban-ip")
                    .requires(ConfigManager.requirePermissionOrOp("banhammer.unpunish.unbanip"))
                    .then(playerArgument("player")
                            .executes(ctx -> removePunishmentCommand(ctx, PunishmentTypes.IPBAN))
                            .then(argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> removePunishmentCommand(ctx, PunishmentTypes.IPBAN))
                            )
                    ));

            dispatcher.register(literal("unmute")
                    .requires(ConfigManager.requirePermissionOrOp("banhammer.unpunish.unmute"))
                    .then(playerArgument("player")
                            .executes(ctx -> removePunishmentCommand(ctx, PunishmentTypes.MUTE))
                            .then(argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> removePunishmentCommand(ctx, PunishmentTypes.MUTE))
                            )
                    ));

            dispatcher.register(literal("pardon")
                    .requires(ConfigManager.requirePermissionOrOp("banhammer.unpunish.pardon"))
                    .then(playerArgument("player")
                            .executes(ctx -> removePunishmentCommand(ctx, null))
                            .then(argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> removePunishmentCommand(ctx, null))
                            )
                    ));
        });
    }

    private static int removePunishmentCommand(CommandContext<ServerCommandSource> ctx, PunishmentTypes type) {
        CompletableFuture.runAsync(() -> {

            MinecraftServer server = ctx.getSource().getServer();
            String playerNameOrIp = ctx.getArgument("player", String.class);


            Config config = ConfigManager.getConfig();

            BHPlayerData player = Helpers.lookupPlayerData(playerNameOrIp);

            if (player == null) {
                ctx.getSource().sendFeedback(new LiteralText("Couldn't find player " + playerNameOrIp + "!").formatted(Formatting.RED), false);
            }

            UUID playerUUID = player.uuid();
            String playerName = player.name();
            String playerIP = player.ip();

            ServerPlayerEntity executor;
            try {
                executor = ctx.getSource().getPlayer();
            } catch (Exception e) {
                executor = null;
            }

            Text message = null;
            String altMessage = "";
            int n = 0;


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

            if (type != null) {
                switch (type) {
                    case BAN:
                        n += BanHammer.removePunishment(playerUUID.toString(), PunishmentTypes.BAN);
                        message = config.unbanChatMessage;
                        altMessage = "This player wasn't banned!";
                        break;
                    case IPBAN:
                        if (playerIP != null) {
                            n += BanHammer.removePunishment(playerIP, PunishmentTypes.IPBAN);
                        }
                        if (type == PunishmentTypes.IPBAN && ConfigManager.getConfig().configData.standardBanPlayersWithBannedIps) {
                            n += BanHammer.removePunishment(playerUUID.toString(), PunishmentTypes.BAN);
                        }
                        message = config.ipUnbanChatMessage;
                        altMessage = "This player wasn't ipbanned!";
                        break;
                    case MUTE:
                        n += BanHammer.removePunishment(playerUUID.toString(), PunishmentTypes.MUTE);
                        message = config.unmuteChatMessage;
                        altMessage = "This player wasn't muted!";
                        break;
                }
            } else {
                if (playerUUID != null) {
                    n += BanHammer.removePunishment(playerUUID.toString(), PunishmentTypes.BAN);
                    n += BanHammer.removePunishment(playerUUID.toString(), PunishmentTypes.MUTE);
                }
                if (playerIP != null) {
                    n += BanHammer.removePunishment(playerIP, PunishmentTypes.IPBAN);
                }

                message = config.pardonChatMessage;
                altMessage = "This player didn't have any punishments!";
            }

            if (n > 0) {
                HashMap<String, Text> list = new HashMap<>();

                list.put("operator", ctx.getSource().getDisplayName());
                list.put("banned", new LiteralText(playerName));
                list.put("banned_uuid", new LiteralText(playerUUID.toString()));
                list.put("reason", new LiteralText(reason));
                Text textMessage = PlaceholderAPI.parsePredefinedText(message, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, list);

                if (config.configData.punishmentsAreSilent || isSilent) {
                    if (player.player() != null) {
                        player.player().sendMessage(textMessage, false);
                    }

                    ctx.getSource().sendFeedback(textMessage, false);
                } else {
                    ctx.getSource().sendFeedback(textMessage, false);

                    for (ServerPlayerEntity player2 : server.getPlayerManager().getPlayerList()) {
                        if (player2 != executor) {
                            player2.sendMessage(textMessage, MessageType.SYSTEM, Util.NIL_UUID);
                        }
                    }
                }

                if (config.webhook != null) {
                    DiscordMessageData.Message tempMessage;

                    DiscordMessageData data = config.discordMessages;
                    if (type != null) {
                        tempMessage = switch (type) {
                            case BAN -> data.sendUnbanMessage ? data.unbanMessage : null;
                            case IPBAN -> data.sendUnbanIpMessage ? data.unBanIpMessage : null;
                            case MUTE -> data.sendUnmuteMessage ? data.unmuteMessage : null;
                            case KICK -> null;
                        };
                    } else {
                        tempMessage = data.sendPardonMessage ? data.pardonMessage : null;
                    }

                    if (tempMessage != null) {
                        Map<String, String> placeholders = new HashMap<>();

                        placeholders.put("operator", ctx.getSource().getDisplayName().getString());
                        placeholders.put("banned", playerName);
                        placeholders.put("banned_uuid", playerUUID.toString());
                        placeholders.put("reason", reason);


                        config.webhook.send(tempMessage.build(placeholders));
                    }
                }
            } else {
                ctx.getSource().sendFeedback(new LiteralText(altMessage).formatted(Formatting.RED), false);
            }
        });

        return 1;
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
