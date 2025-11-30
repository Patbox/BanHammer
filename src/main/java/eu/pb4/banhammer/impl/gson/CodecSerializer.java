package eu.pb4.banhammer.impl.gson;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import eu.pb4.banhammer.impl.BanHammerImpl;
import java.lang.reflect.Type;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public record CodecSerializer<T>(Codec<T> codec) implements JsonSerializer<T>, JsonDeserializer<T> {
        public static CodecSerializer<Component> TEXT = new CodecSerializer<>(ComponentSerialization.CODEC);

        public String toJsonString(T text) {
            return serialize(text, Component.class, null).toString();
        }

        public T fromJson(String string) {
            return deserialize(JsonParser.parseString(string), Component.class, null);
        }

        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return this.codec.decode(BanHammerImpl.SERVER != null ? BanHammerImpl.SERVER.registryAccess().createSerializationContext(JsonOps.INSTANCE) : JsonOps.INSTANCE, json)
                        .getOrThrow().getFirst();
            } catch (Throwable e) {
                return null;
            }
        }

        @Override
        public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
            try {
                return src != null ? this.codec.encodeStart(BanHammerImpl.SERVER != null ? BanHammerImpl.SERVER.registryAccess().createSerializationContext(JsonOps.INSTANCE) : JsonOps.INSTANCE, src)
                        .getOrThrow() : JsonNull.INSTANCE;
            } catch (Throwable e) {
                return JsonNull.INSTANCE;
            }
        }
    }