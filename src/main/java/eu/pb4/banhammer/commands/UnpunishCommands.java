package eu.pb4.banhammer.commands;


import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.banhammer.BanHammerMod;
import eu.pb4.banhammer.Helpers;
import eu.pb4.banhammer.config.Config;
import eu.pb4.banhammer.config.ConfigManager;
import eu.pb4.banhammer.types.BHPlayerData;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.literal;

public class UnpunishCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("unban")
                    .requires(Permissions.require("banhammer.unpunish.unban", 1))
                    .then(playerArgument("player")
                            .executes(ctx -> removePunishmentCommand(ctx, PunishmentTypes.BAN))
                    ));

            dispatcher.register(literal("unban-ip")
                    .requires(Permissions.require("banhammer.unpunish.unbanip", 1))
                    .then(playerArgument("player")
                            .executes(ctx -> removePunishmentCommand(ctx, PunishmentTypes.IPBAN))
                    ));

            dispatcher.register(literal("unmute")
                    .requires(Permissions.require("banhammer.unpunish.unmute", 1))
                    .then(playerArgument("player")
                            .executes(ctx -> removePunishmentCommand(ctx, PunishmentTypes.MUTE))
                    ));

            dispatcher.register(literal("pardon")
                    .requires(Permissions.require("banhammer.unpunish.pardon", 1))
                    .then(playerArgument("player")
                            .executes(ctx -> removePunishmentCommand(ctx, null))
                    ));
        });
    }

    private static int removePunishmentCommand(CommandContext<ServerCommandSource> ctx, PunishmentTypes type) {
        CompletableFuture.runAsync(() -> {

            MinecraftServer server = ctx.getSource().getMinecraftServer();
            String playerNameOrIp = ctx.getArgument("player", String.class);

            Config config = ConfigManager.getConfig();

            BHPlayerData player = Helpers.lookupPlayerData(playerNameOrIp, type);

            if (player == null) {
                ctx.getSource().sendFeedback(new LiteralText("Couldn't find player " + playerNameOrIp + "!").formatted(Formatting.RED), false);
            }

            UUID playerUUID = player.uuid;
            Text playerDisplay = player.displayName;
            String playerName = player.name;
            String playerIP = player.ip;

            ServerPlayerEntity executor;
            try {
                executor = ctx.getSource().getPlayer();
            } catch (Exception e) {
                executor = null;
            }

            String message = null;
            String altMessage = "";
            int n = 0;

            if (type != null) {
                switch (type) {
                    case BAN:
                        n += BanHammerMod.removePunishment(playerUUID.toString(), PunishmentTypes.BAN);
                        message = config.unbanChatMessage;
                        altMessage = "This player wasn't banned!";
                        break;
                    case IPBAN:
                        n += BanHammerMod.removePunishment(playerIP, PunishmentTypes.IPBAN);
                        if (type == PunishmentTypes.IPBAN && ConfigManager.getConfig().configData.standardBanPlayersWithBannedIps) {
                            n += BanHammerMod.removePunishment(playerUUID.toString(), PunishmentTypes.BAN);
                        }
                        message = config.ipUnbanChatMessage;
                        altMessage = "This player wasn't ipbanned!";
                        break;
                    case MUTE:
                        n += BanHammerMod.removePunishment(playerUUID.toString(), PunishmentTypes.MUTE);
                        message = config.unmuteChatMessage;
                        altMessage = "This player wasn't muted!";
                        break;
                }
            } else {
                n += BanHammerMod.removePunishment(playerUUID.toString(), PunishmentTypes.BAN);
                n += BanHammerMod.removePunishment(playerIP, PunishmentTypes.IPBAN);
                n += BanHammerMod.removePunishment(playerUUID.toString(), PunishmentTypes.MUTE);

                message = config.pardonChatMessage;
                altMessage = "This player didn't have any punishments!";
            }

            if (n > 0) {
                ArrayList<Template> list = new ArrayList<>();

                list.add(Template.of("operator", BanHammerMod.getAdventure().toAdventure(ctx.getSource().getDisplayName())));
                list.add(Template.of("banned", playerName));

                Text textMessage = Helpers.parseMessage(message, list);

                if (config.configData.punishmentsAreSilent) {
                    if (player.player != null) {
                        player.player.sendMessage(textMessage, false);
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

                    for (String player : ctx.getSource().getMinecraftServer().getPlayerNames()) {
                        if (player.toLowerCase(Locale.ROOT).contains(remaining)) {
                            builder.suggest(player);
                        }
                    }

                    return builder.buildFuture();
                });
    }
}
