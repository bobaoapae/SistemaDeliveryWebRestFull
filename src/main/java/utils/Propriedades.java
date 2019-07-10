package utils;

import javax.servlet.ServletContext;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class Propriedades {

    private static Map<String, String> propriedades;

    public static void inicializar(ServletContext servletContext) {
        propriedades = new HashMap<>();
        Enumeration<String> propertysNames = servletContext.getInitParameterNames();
        while (propertysNames.hasMoreElements()) {
            String property = propertysNames.nextElement();
            propriedades.put(property, servletContext.getInitParameter(property));
        }
    }

    public static String getProperty(String propertyName) {
        if (propriedades.containsKey(propertyName)) {
            return propriedades.get(propertyName);
        } else {
            return null;
        }
    }

    public static String pathCacheWebWhats() {
        boolean producao = Boolean.getBoolean(getProperty("producao"));
        String path = getProperty("cache-web-whats");
        if (!producao) {
            path += "-homologacao";
        }
        return path += "\\";
    }

    public static String pathLogs() {
        boolean producao = Boolean.getBoolean(getProperty("producao"));
        String path = getProperty("logs");
        if (!producao) {
            path += "-homologacao";
        }
        return path += "\\";
    }
}
