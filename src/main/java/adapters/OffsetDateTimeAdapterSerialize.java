package adapters;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeAdapterSerialize implements JsonSerializer<OffsetDateTime> {

    @Override
    public JsonElement serialize(OffsetDateTime date, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))); // "yyyy-mm-dd"
    }
}