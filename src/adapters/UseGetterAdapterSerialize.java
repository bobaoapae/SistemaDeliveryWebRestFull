package adapters;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import restFul.controle.ControleSistema;
import utils.Utilitarios;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class UseGetterAdapterSerialize<K> implements JsonSerializer<K> {
    @Override
    public JsonElement serialize(K o, Type type, JsonSerializationContext jsonSerializationContext) {
        Gson builder = Utilitarios.getDefaultGsonBuilder(type).create();
        JsonElement element = builder.toJsonTree(o, type);
        HashMap<String, JsonElement> valoresAtualizar = new HashMap<>();
        if (o.getClass().isAnnotationPresent(ExposeGetter.class)) {
            ExposeGetter[] exposeGetters = o.getClass().getAnnotationsByType(ExposeGetter.class);
            for (ExposeGetter exposeGetter : exposeGetters) {
                try {
                    Method method = o.getClass().getMethod(exposeGetter.methodName());
                    valoresAtualizar.put(exposeGetter.nameExpose(), jsonSerializationContext.serialize(method.invoke(o)));
                } catch (NoSuchMethodException e) {
                    ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
                } catch (InvocationTargetException e) {
                    ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
        for (Map.Entry<String, JsonElement> entry : valoresAtualizar.entrySet()) {
            element.getAsJsonObject().add(entry.getKey(), entry.getValue());
        }
        return element;
    }
}
