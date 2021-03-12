package eu.pb4.banhammer.commands;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.banhammer.BanHammerMod;
import eu.pb4.banhammer.Helpers;
import eu.pb4.banhammer.config.Config;
import eu.pb4.banhammer.config.ConfigManager;
import eu.pb4.banhammer.types.BasicPunishment;
import eu.pb4.banhammer.types.PunishmentTypes;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.kyori.adventure.text.minimessage.Template;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;


import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Commands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("kick")
                    .requires(Permissions.require("banhammer.kick", 1))
                    .then(playerArgument("player")
                            .executes(ctx -> punishCommand(ctx, false, PunishmentTypes.KICK))
                            .then(argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> punishCommand(ctx,false, PunishmentTypes.KICK))
                            )
                    ));

            dispatcher.register(literal("mute")
                    .requires(Permissions.require("banhammer.mute", 1))
                    .then(playerArgument("player")
                            .executes(ctx -> punishCommand(ctx, false, PunishmentTypes.MUTE))
                            .then(argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> punishCommand(ctx, false, PunishmentTypes.MUTE))
                            )
            ));

            dispatcher.register(literal("ban")
                    .requires(Permissions.require("banhammer.ban", 1))
                    .then(playerArgument("player")
                            .executes(ctx -> punishCommand(ctx, false, PunishmentTypes.BAN))
                            .then(argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> punishCommand(ctx,false, PunishmentTypes.BAN))
                            )
            ));

            dispatcher.register(literal("ban-ip")
                    .requires(Permissions.require("banhammer.banip", 1))
                    .then(playerArgument("player")
                            .executes(ctx -> punishCommand(ctx, false, PunishmentTypes.IPBAN))
                            .then(argument("reason", StringArgumentType.greedyString())
                                    .executes(ctx -> punishCommand(ctx,false, PunishmentTypes.IPBAN))
                            )
            ));

            dispatcher.register(literal("tempmute")
                    .requires(Permissions.require("banhammer.tempmute", 1))
                    .then(playerArgument("player")
                            .then(argument("duration", StringArgumentType.word())
                                    .executes(ctx -> punishCommand(ctx, true, PunishmentTypes.MUTE))
                                    .then(argument("reason", StringArgumentType.greedyString())
                                            .executes(ctx -> punishCommand(ctx, true, PunishmentTypes.MUTE))
                            ))
                    ));

            dispatcher.register(literal("tempban")
                    .requires(Permissions.require("banhammer.tempban", 1))
                    .then(playerArgument("player")
                            .then(argument("duration", StringArgumentType.word())
                                    .executes(ctx -> punishCommand(ctx,true, PunishmentTypes.BAN))
                                    .then(argument("reason", StringArgumentType.greedyString())
                                            .executes(ctx -> punishCommand(ctx,true, PunishmentTypes.BAN))
                                    )
                            )
                    ));

            dispatcher.register(literal("tempban-ip")
                    .requires(Permissions.require("banhammer.tempbanip", 1))
                    .then(playerArgument("player")
                            .then(argument("duration", StringArgumentType.word())
                                    .executes(ctx -> punishCommand(ctx, true, PunishmentTypes.IPBAN))
                                    .then(argument("reason", StringArgumentType.greedyString())
                                            .executes(ctx -> punishCommand(ctx,true, PunishmentTypes.IPBAN))
                            ))
                    ));

            dispatcher.register(literal("unban")
                    .requires(Permissions.require("banhammer.unban", 1))
                    .then(playerArgument("player")
                            .executes(ctx -> removePunishmentCommand(ctx, PunishmentTypes.BAN))
                    ));

            dispatcher.register(literal("unban-ip")
                    .requires(Permissions.require("banhammer.unban", 1))
                    .then(playerArgument("player")
                            .executes(ctx -> removePunishmentCommand(ctx, PunishmentTypes.IPBAN))
                    ));

            dispatcher.register(literal("unmute")
                    .requires(Permissions.require("banhammer.unmute", 1))
                    .then(playerArgument("player")
                            .executes(ctx -> removePunishmentCommand(ctx, PunishmentTypes.MUTE))
                    ));

            dispatcher.register(literal("pardon")
                    .requires(Permissions.require("banhammer.pardon", 1))
                    .then(playerArgument("player")
                            .executes(ctx -> removePunishmentCommand(ctx, null))
                    ));
        });
    }

    private static int punishCommand(CommandContext<ServerCommandSource> ctx, boolean isTemp, PunishmentTypes type) {
        MinecraftServer server = ctx.getSource().getMinecraftServer();
        Config config = ConfigManager.getConfig();
        String playerName = ctx.getArgument("player", String.class);

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
                return 0;
            }
        }


        String reason;

        try {
            reason = ctx.getArgument("reason", String.class);
        } catch (Exception e) {
            reason = config.defaultReason;
        }

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);

        UUID playerUUID = null;
        Text playerNameText;
        String playerNameRaw;
        String playerIP = null;

        if (player != null) {
            playerUUID = player.getUuid();
            playerIP = player.getIp();
            playerNameText = player.getDisplayName();
            playerNameRaw = player.getGameProfile().getName();
        } else if (type.ipBased && InetAddresses.isInetAddress(playerName)) {
            GameProfile profile = null;

            for (Map.Entry<String, String> entry : BanHammerMod.IP_CACHE.entrySet()) {
                if (entry.getValue().equals(playerName)) {
                    playerUUID = UUID.fromString(entry.getKey());
                    playerIP = entry.getKey();
                    profile = server.getUserCache().getByUuid(playerUUID);
                    break;
                }
            }

            if (profile == null) {
                playerIP = playerName;
                playerNameRaw = "Unknown player";
                playerNameText = new LiteralText("Unknown player").formatted(Formatting.ITALIC);
                playerUUID = UUID.fromString("00000000-0000-4000-0000-000000000000");
            } else {
                playerNameText = new LiteralText(profile.getName());
                playerNameRaw = profile.getName();
            }
        } else {
            GameProfile profile = server.getUserCache().findByName(playerName);

            if (profile != null) {
                playerUUID = profile.getId();
                playerIP = BanHammerMod.IP_CACHE.get(playerUUID.toString());
                playerNameText = new LiteralText(profile.getName());
                playerNameRaw = profile.getName();
            } else {
                ctx.getSource().sendError(new LiteralText("Couldn't find player!"));
                return 0;
            }
        }

        ServerPlayerEntity executor;
        UUID executorUUID;
        try {
            executor = ctx.getSource().getPlayer();
            executorUUID = executor.getUuid();
        } catch (Exception e) {
            executorUUID = null;
        }

        BasicPunishment punishment = new BasicPunishment(playerUUID, playerIP, playerNameText, playerNameRaw, executorUUID, ctx.getSource().getDisplayName(), System.currentTimeMillis() / 1000, duration, reason, type);

        BanHammerMod.punishPlayer(punishment, config.configData.punishmentsAreSilent);

        if (config.configData.punishmentsAreSilent && !Permissions.check(ctx.getSource(), "banhammer.seesilent", 1)) {
            ctx.getSource().sendFeedback(punishment.getChatMessage(), false);
        }

        return 1;
    }

    private static int removePunishmentCommand(CommandContext<ServerCommandSource> ctx, PunishmentTypes type) {
        MinecraftServer server = ctx.getSource().getMinecraftServer();

        String playerName = ctx.getArgument("player", String.class);

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);

        Config config = ConfigManager.getConfig();

        UUID playerUUID = null;
        String playerIP = null;

        if (player != null) {
            playerUUID = player.getUuid();
            playerIP = player.getIp();
        } else if (type != null && type.ipBased && InetAddresses.isInetAddress(playerName)) {
            GameProfile profile = null;
            for (Map.Entry<String, String> entry : BanHammerMod.IP_CACHE.entrySet()) {
                if (entry.getValue().equals(playerName)) {
                    playerUUID = UUID.fromString(entry.getKey());
                    playerIP = entry.getKey();
                    profile = server.getUserCache().getByUuid(playerUUID);
                    break;
                }
            }
            if (profile == null) {
                playerIP = playerName;
                playerUUID = UUID.fromString("00000000-0000-4000-0000-000000000000");
            }
        } else {
            GameProfile profile = server.getUserCache().findByName(playerName);
            if (profile != null) {
                playerUUID = profile.getId();
                playerIP = BanHammerMod.IP_CACHE.get(playerUUID.toString());
            } else {
                ctx.getSource().sendError(new LiteralText("Couldn't find player!"));
                return 0;
            }
        }

        ServerPlayerEntity executor;
        try {
            executor = ctx.getSource().getPlayer();
        } catch (Exception e) {
            executor = null;
        }

        String message = null;

        if (type != null) {
            switch (type) {
                case BAN:
                    BanHammerMod.removePunishment(playerUUID.toString(), PunishmentTypes.BAN);
                    message = config.unbanChatMessage;
                    break;
                case IPBAN:
                    BanHammerMod.removePunishment(playerIP, PunishmentTypes.IPBAN);
                    message = config.ipUnbanChatMessage;
                    break;
                case MUTE:
                    BanHammerMod.removePunishment(playerUUID.toString(), PunishmentTypes.MUTE);
                    message = config.unmuteChatMessage;
                    break;
            }
        } else {
            BanHammerMod.removePunishment(playerUUID.toString(), PunishmentTypes.BAN);
            BanHammerMod.removePunishment(playerIP, PunishmentTypes.IPBAN);
            BanHammerMod.removePunishment(playerUUID.toString(), PunishmentTypes.MUTE);
            message = config.pardonChatMessage;
        }

        ArrayList<Template> list = new ArrayList<>();

        list.add(Template.of("operator", BanHammerMod.getAdventure().toAdventure(ctx.getSource().getDisplayName())));
        list.add(Template.of("banned", playerName));

        Text textMessage = Helpers.parseMessage(message, list);

        if (config.configData.punishmentsAreSilent) {
            if (player != null) {
                player.sendMessage(textMessage, false);
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
