package adapters;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

public class TimestampAdapterSerialize implements JsonSerializer<Timestamp> {

    @Override
    public JsonElement serialize(Timestamp timestamp, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(timestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))); // "yyyy-mm-dd"
    }
}
