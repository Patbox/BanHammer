package eu.pb4.banhammer.mixin.vanilla;

import net.minecraft.server.commands.BanIpCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BanIpCommands.class)
public class BanIpCommandMixin {
    @ModifyArg(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;literal(Ljava/lang/String;)Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;"), index = 0, require = 0)
    private static String banHammer_renameCommand(String def) {
        return "minecraft:ban-ip";
    }
}
