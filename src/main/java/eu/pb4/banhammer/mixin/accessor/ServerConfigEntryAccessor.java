package eu.pb4.banhammer.mixin.accessor;

import net.minecraft.server.ServerConfigEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerConfigEntry.class)
public interface ServerConfigEntryAccessor<T> {
    @Accessor("key")
    T getKey();

}
