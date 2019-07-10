package adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeAdapterDeserialize implements JsonDeserializer<Time> {
    @Override
    public Time deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement.isJsonNull() || jsonElement.getAsString().isEmpty()) {
            return null;
        }
        return Time.valueOf(LocalTime.parse(jsonElement.getAsString(), DateTimeFormatter.ofPattern("HH:mm")));
    }
}
