package restFul.filtros;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang.exception.ExceptionUtils;
import restFul.controle.ControleSessions;
import restFul.controle.ControleSistema;
import restFul.controle.ControleTokens;
import restFul.modelo.Token;
import restFul.modelo.TokenSecurityContext;
import utils.DateUtils;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;

@Provider
@PreMatching
public class AuthenticationToken implements ContainerRequestFilter {


    private Gson gson = new Gson();

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        String path = containerRequestContext.getUriInfo().getPath();
        if (path.startsWith("/api")) {
            MultivaluedMap<String, String> queryParameters = containerRequestContext.getUriInfo().getQueryParameters();
            if (queryParameters.containsKey("token") || containerRequestContext.getCookies().containsKey("token")) {
                String token = "";
                if (queryParameters.containsKey("token")) {
                    token = queryParameters.get("token").get(0);
                } else {
                    token = containerRequestContext.getCookies().get("token").getValue();
                }
                Token k = null;
                try {
                    k = ControleTokens.getInstance().getToken(token);
                } catch (SQLException e) {
                    ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
                    containerRequestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build());
                    return;
                }
                if (k != null) {
                    if (DateUtils.isAfterDay(k.getValidade(), new Date())) {
                        TokenSecurityContext securityContext = new TokenSecurityContext(k);
                        containerRequestContext.setSecurityContext(securityContext);
                        k.setSistemaDelivery(ControleSessions.getInstance().getSessionForEstabelecimento(k.getEstabelecimento()));
                    } else {
                        JsonObject ob = new JsonObject();
                        ob.addProperty("status", "tokenExpired");
                        containerRequestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON).entity(gson.toJson(ob)).build());
                    }
                } else {
                    JsonObject ob = new JsonObject();
                    ob.addProperty("status", "tokenInvalid");
                    containerRequestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON).entity(gson.toJson(ob)).build());
                }
            } else {
                JsonObject ob = new JsonObject();
                ob.addProperty("status", "tokenMissing");
                containerRequestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(gson.toJson(ob)).build());
            }
        } else if (path.startsWith("/adm")) {
            MultivaluedMap<String, String> queryParameters = containerRequestContext.getUriInfo().getQueryParameters();
            if (queryParameters.containsKey("securePass")) {
                String securePass = queryParameters.get("securePass").get(0);
                try {
                    if (!securePass.equals(ControleSistema.getInstance().getSecurePass())) {
                        JsonObject ob = new JsonObject();
                        ob.addProperty("status", "securePass invalid");
                        containerRequestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON).entity(gson.toJson(ob)).build());
                    }
                } catch (SQLException e) {
                    ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
                    containerRequestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build());
                    return;
                }
            } else {
                JsonObject ob = new JsonObject();
                ob.addProperty("status", "securePass missing");
                containerRequestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(gson.toJson(ob)).build());
            }
        }
    }
}
