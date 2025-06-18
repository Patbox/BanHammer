package eu.pb4.banhammer.impl.gson;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class LowercaseEnumTypeAdapterFactory implements TypeAdapterFactory {
    public LowercaseEnumTypeAdapterFactory() {
    }

    @Nullable
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        var class_ = typeToken.getRawType();
        if (!class_.isEnum()) {
            return null;
        } else {
            final Map<String, T> map = Maps.newHashMap();
            Object[] var5 = class_.getEnumConstants();
            int var6 = var5.length;

            for (int var7 = 0; var7 < var6; ++var7) {
                var object = var5[var7];
                map.put(this.getKey(object), (T) object);
            }

            return new TypeAdapter<T>() {
                public void write(JsonWriter writer, T o) throws IOException {
                    if (o == null) {
                        writer.nullValue();
                    } else {
                        writer.value(LowercaseEnumTypeAdapterFactory.this.getKey(o));
                    }

                }

                @Nullable
                public T read(JsonReader reader) throws IOException {
                    if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull();
                        return null;
                    } else {
                        return map.get(reader.nextString());
                    }
                }
            };
        }
    }

    String getKey(Object o) {
        return o instanceof Enum ? ((Enum) o).name().toLowerCase(Locale.ROOT) : o.toString().toLowerCase(Locale.ROOT);
    }
}