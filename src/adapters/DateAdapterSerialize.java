package adapters;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.sql.Date;
import java.time.format.DateTimeFormatter;

public class DateAdapterSerialize implements JsonSerializer<Date> {

    @Override
    public JsonElement serialize(Date date, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(date.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))); // "yyyy-mm-dd"
    }
}