package eu.pb4.banhammer.mixin.accessor;

import net.minecraft.server.BanEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Date;

@Mixin(BanEntry.class)
public interface BanEntryAccessor {
    @Accessor("creationDate")
    Date getCreationDate();
}
