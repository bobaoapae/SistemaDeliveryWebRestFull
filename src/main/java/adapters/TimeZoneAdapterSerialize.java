package adapters;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.TimeZone;

public class TimeZoneAdapterSerialize implements JsonSerializer<TimeZone> {

    @Override
    public JsonElement serialize(TimeZone timeZone, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(timeZone.toZoneId().getDisplayName(TextStyle.NARROW, Locale.forLanguageTag("pt-BR")));
    }
}
