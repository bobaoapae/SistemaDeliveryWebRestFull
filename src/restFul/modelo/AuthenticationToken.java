package restFul.modelo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import restFul.controle.ControleSessions;
import restFul.controle.ControleTokens;
import utils.DateUtils;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Date;

@Provider
@PreMatching
public class AuthenticationToken implements ContainerRequestFilter {


    Gson gson = new Gson();

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
                Token k = ControleTokens.getInstance().getToken(token);
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
        }
    }
}
