package eu.pb4.banhammer.mixin.accessor;

import net.minecraft.server.players.StoredUserEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StoredUserEntry.class)
public interface ServerConfigEntryAccessor<T> {
    @Accessor("user")
    T getKeyServer();

}
