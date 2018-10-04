package adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimestampAdapterDeserialize implements JsonDeserializer<Timestamp> {
    @Override
    public Timestamp deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return Timestamp.valueOf(LocalDateTime.parse(jsonElement.getAsString(), DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
    }
}
