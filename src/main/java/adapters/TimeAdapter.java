package adapters;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.sql.Time;
import java.time.format.DateTimeFormatter;

public class TimeAdapter implements JsonSerializer<Time> {

    @Override
    public JsonElement serialize(Time time, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(time.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))); // "yyyy-mm-dd"
    }
}