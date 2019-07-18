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
        String path = getProperty("cache-web-whats") + getEstadoServidor().name().toLowerCase();
        return path + "\\";
    }

    public static String pathLogs() {
        String path = getProperty("logs") + getEstadoServidor().name().toLowerCase();
        return path + "\\";
    }

    public static String pathBinarios() {
        String path = getProperty("binarios") + getEstadoServidor().name().toLowerCase();
        return path + "\\";
    }

    public static EstadoServidor getEstadoServidor() {
        return EstadoServidor.valueOf(getProperty("estadoServidor").toUpperCase());
    }

    public enum EstadoServidor {
        PRODUCAO,
        HOMOLOGACAO,
        TESTES
    }
}
