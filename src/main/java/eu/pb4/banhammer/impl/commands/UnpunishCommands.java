package eu.pb4.banhammer.impl.commands;


import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.banhammer.api.PunishmentType;
import eu.pb4.banhammer.impl.BHUtils;
import eu.pb4.banhammer.impl.BanHammerImpl;
import eu.pb4.banhammer.impl.config.Config;
import eu.pb4.banhammer.impl.config.ConfigManager;
import eu.pb4.banhammer.impl.config.data.DiscordMessageData;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.node.TextNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class UnpunishCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(create("unban", PunishmentType.BAN));
            dispatcher.register(create("unban-ip", PunishmentType.IP_BAN));
            dispatcher.register(create("unmute", PunishmentType.MUTE));
            dispatcher.register(create("unwarn", PunishmentType.WARN));
            dispatcher.register(create("pardon", null));
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> create(String command, PunishmentType type) {
        return literal(command)
                .requires(ConfigManager.requirePermissionOrOp("banhammer.unpunish." + command))
                .then(GeneralCommands.playerArgument("player")
                        .executes(ctx -> removePunishmentCommand(ctx, type))
                        .then(argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> removePunishmentCommand(ctx, type))
                        )
                );
    }

    private static int removePunishmentCommand(CommandContext<CommandSourceStack> ctx, PunishmentType type) {
        CompletableFuture.runAsync(() -> {

            var config = ConfigManager.getConfig();

            var playerNameOrIp = ctx.getArgument("player", String.class);
            var players = BHUtils.lookupPlayerData(playerNameOrIp, ctx.getSource().getServer());

            if (players.isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal("Couldn't find player " + playerNameOrIp + "!").withStyle(ChatFormatting.RED), false);
            }

            ServerPlayer executor;
            try {
                executor = ctx.getSource().getPlayer();
            } catch (Exception e) {
                executor = null;
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

            for (var player : players) {
                TextNode message = null;
                String altMessage = "";
                int n = 0;

                if (type != null) {
                    switch (type) {
                        case BAN:
                            n += BanHammerImpl.removePunishment(player.uuid().toString(), PunishmentType.BAN);
                            message = config.unbanChatMessage;
                            altMessage = "This player wasn't banned!";
                            break;
                        case IP_BAN:
                            if (player.ip() != null) {
                                n += BanHammerImpl.removePunishment(player.ip(), PunishmentType.IP_BAN);
                            }
                            if (type == PunishmentType.IP_BAN && ConfigManager.getConfig().configData.standardBanPlayersWithBannedIps) {
                                n += BanHammerImpl.removePunishment(player.uuid().toString(), PunishmentType.BAN);
                            }
                            message = config.ipUnbanChatMessage;
                            altMessage = "This player wasn't ipbanned!";
                            break;
                        case MUTE:
                            n += BanHammerImpl.removePunishment(player.uuid().toString(), PunishmentType.MUTE);
                            message = config.unmuteChatMessage;
                            altMessage = "This player wasn't muted!";
                            break;
                        case WARN:
                            n += BanHammerImpl.removePunishment(player.uuid().toString(), PunishmentType.WARN);
                            message = config.unwarnChatMessage;
                            altMessage = "This player wasn't warned!";
                            break;
                    }
                } else {
                    if (player.uuid() != null) {
                        var uuid = player.uuid().toString();
                        n += BanHammerImpl.removePunishment(uuid, PunishmentType.BAN);
                        n += BanHammerImpl.removePunishment(uuid, PunishmentType.WARN);
                        n += BanHammerImpl.removePunishment(uuid, PunishmentType.MUTE);
                    }
                    if (player.ip() != null) {
                        n += BanHammerImpl.removePunishment(player.ip(), PunishmentType.IP_BAN);
                    }

                    message = config.pardonChatMessage;
                    altMessage = "This player didn't have any punishments!";
                }

                if (n > 0) {
                    HashMap<String, Component> list = new HashMap<>();

                    list.put("operator", ctx.getSource().getDisplayName());
                    list.put("banned", Component.literal(player.name()));
                    list.put("banned_uuid", Component.literal(player.uuid().toString()));
                    list.put("reason", Component.literal(reason));
                    Component textMessage = message.toText(player.placeholderContext(ctx.getSource().getServer()).asParserContext().with(Config.PLACEHOLDER, list::get));

                    if (config.configData.punishmentsAreSilent || isSilent) {
                        if (player.player() != null) {
                            player.player().displayClientMessage(textMessage, false);
                        }

                        ctx.getSource().sendSuccess(() -> textMessage, false);
                    } else {
                        ctx.getSource().sendSuccess(() -> textMessage, false);

                        for (ServerPlayer player2 : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                            if (player2 != executor) {
                                player2.sendSystemMessage(textMessage);
                            }
                        }
                    }

                    if (!config.webhooks.isEmpty()) {
                        DiscordMessageData.Message tempMessage;
                        var data = config.discordMessages;
                        if (type != null) {
                            tempMessage = switch (type) {
                                case BAN -> data.sendUnbanMessage ? data.unbanMessage : null;
                                case IP_BAN -> data.sendUnbanIpMessage ? data.unBanIpMessage : null;
                                case MUTE -> data.sendUnmuteMessage ? data.unmuteMessage : null;
                                case WARN -> data.sendUnwarnMessage ? data.unwarnMessage : null;
                                case KICK -> null;
                            };
                        } else {
                            tempMessage = data.sendPardonMessage ? data.pardonMessage : null;
                        }

                        if (tempMessage != null) {
                            var placeholders = new HashMap<String, String>();

                            placeholders.put("operator", ctx.getSource().getDisplayName().getString());
                            placeholders.put("banned", player.name());
                            placeholders.put("banned_uuid", player.uuid().toString());
                            placeholders.put("reason", reason);

                            var msg = HttpRequest.BodyPublishers.ofString(tempMessage.build(placeholders));

                            for (var hook : config.webhooks) {
                                BanHammerImpl.HTTP_CLIENT.sendAsync(HttpRequest.newBuilder()
                                        .uri(hook)
                                        .headers("Content-Type", "application/json")
                                        .POST(msg).build(), HttpResponse.BodyHandlers.discarding());
                            }
                        }
                    }
                } else {
                    String finalAltMessage = altMessage;
                    ctx.getSource().sendSuccess(() -> Component.literal(finalAltMessage).withStyle(ChatFormatting.RED), false);
                }
            }
        });

        return 1;
    }


}
