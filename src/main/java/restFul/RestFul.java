package restFul;

import restFul.filtros.AuthenticationToken;
import restFul.filtros.CorsFilter;
import restFul.modelo.ListennerServer;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class RestFul extends Application {

    public RestFul() {
        super();
    }

    @Override
    public Set<Class<?>> getClasses() {
        HashSet h = new HashSet<Class<?>>();
        h.add(ListennerServer.class);
        h.add(AuthenticationToken.class);
        h.add(CorsFilter.class);
        h.add(Manager.class);
        h.add(API.class);
        h.add(Adm.class);
        return h;
    }
}
