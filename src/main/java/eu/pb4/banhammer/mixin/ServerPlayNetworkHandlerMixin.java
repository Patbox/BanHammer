package eu.pb4.banhammer.mixin;

import eu.pb4.banhammer.api.PunishmentType;
import eu.pb4.banhammer.impl.BanHammerImpl;
import eu.pb4.banhammer.impl.config.ConfigManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.message.LastSeenMessageList;
import net.minecraft.network.message.MessageChain;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;
import java.util.Optional;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin extends ServerCommonNetworkHandler {
    @Shadow public ServerPlayerEntity player;

    public ServerPlayNetworkHandlerMixin(MinecraftServer server, ClientConnection connection, ConnectedClientData clientData) {
        super(server, connection, clientData);
    }

    @Shadow protected abstract SignedMessage getSignedMessage(ChatMessageC2SPacket packet, LastSeenMessageList lastSeenMessages) throws MessageChain.MessageChainException;

    @Shadow protected abstract void handleMessageChainException(MessageChain.MessageChainException exception);

    @Shadow protected abstract Optional<LastSeenMessageList> validateAcknowledgment(LastSeenMessageList.Acknowledgment acknowledgment);

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    private void banHammer_checkIfMuted(ChatMessageC2SPacket packet, CallbackInfo ci) {
        boolean blocked = false;
        for (var punishment : BanHammerImpl.CACHED_PUNISHMENTS) {
            if (!punishment.isExpired() && punishment.type == PunishmentType.MUTE && punishment.playerUUID.equals(this.player.getUuid())) {
                this.player.sendMessage(punishment.getDisconnectMessage(), false);
                ci.cancel();
                blocked = true;
            }
        }

        if (!blocked) {
            var punishments = BanHammerImpl.getPlayersPunishments(this.player.getUuid().toString(), PunishmentType.MUTE);
            if (punishments.size() > 0) {
                var punishment = punishments.get(0);

                this.player.sendMessage(punishment.getDisconnectMessage(), false);
                ci.cancel();
                blocked = true;
            }
        }

        if (blocked) {
            Optional<LastSeenMessageList> optional = this.validateAcknowledgment(packet.acknowledgment());
            if (optional.isPresent()) {
                this.server.submit(() -> {
                    try {
                        this.getSignedMessage(packet, (LastSeenMessageList)optional.get());
                    } catch (MessageChain.MessageChainException var6) {
                        this.handleMessageChainException(var6);
                    }
                });
            }
        }
    }

    @Inject(method = "onCommandExecution", at = @At("HEAD"), cancellable = true)
    private void banHammer_checkIfMutedCommand(CommandExecutionC2SPacket packet, CallbackInfo ci) {
        var string = packet.command();

        int x = string.indexOf(" ");
        String rawCommand = string.substring(0, x != -1 ? x : string.length());
        for (String command : ConfigManager.getConfig().mutedCommands) {
            if (rawCommand.equals(command)) {
                for (var punishment : BanHammerImpl.CACHED_PUNISHMENTS) {
                    if (!punishment.isExpired() && punishment.type == PunishmentType.MUTE && punishment.playerUUID.equals(this.player.getUuid())) {
                        this.player.sendMessage(punishment.getDisconnectMessage(), false);
                        ci.cancel();
                        return;
                    }
                }

                var punishments = BanHammerImpl.getPlayersPunishments(this.player.getUuid().toString(), PunishmentType.MUTE);
                if (punishments.size() > 0) {
                    var punishment = punishments.get(0);


                    ci.cancel();
                    this.player.sendMessage(punishment.getDisconnectMessage(), false);
                }

                return;
            }
        }
    }
}
