package eu.pb4.banhammer.mixin.vanilla;

import net.minecraft.server.command.KickCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(KickCommand.class)
public class KickCommandMixin {
    @ModifyArg(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/command/CommandManager;literal(Ljava/lang/String;)Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;"), index = 0)
    private static String renameCommand(String def) {
        return "vanilla-kick";
    }
}
