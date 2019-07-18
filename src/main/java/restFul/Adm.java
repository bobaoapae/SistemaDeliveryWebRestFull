package restFul;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import modelo.Chat;
import modelo.EstadoDriver;
import org.apache.commons.lang.exception.ExceptionUtils;
import restFul.controle.ControleSessions;
import restFul.controle.ControleSistema;
import sistemaDelivery.SistemaDelivery;
import sistemaDelivery.controle.ControleEstabelecimentos;
import utils.Utilitarios;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;
import java.util.logging.Level;

@Path("/adm")
public class Adm {

    JsonParser parser;
    private Gson builder;


    public Adm() {
        parser = new JsonParser();
        builder = Utilitarios.getDefaultGsonBuilder(null).create();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/reiniciarSistema")
    public Response reiniciarSistema(@QueryParam("uuid") @DefaultValue("") String uuid) {
        try {
            ControleSessions.getInstance().finalizarSessionForEstabelecimento(ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(UUID.fromString(uuid)));
            Thread.sleep(2000);
            ControleSessions.getInstance().getSessionForEstabelecimento(ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(UUID.fromString(uuid)));
            return Response.status(Response.Status.OK).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        } catch (Exception e) {
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/status")
    public Response status(@DefaultValue("false") @QueryParam("msgTeste") boolean msgTeste) {
        try {
            JsonArray jsonArray = new JsonArray();
            for (SistemaDelivery sistemaDelivery : ControleSessions.getInstance().getSessionsAtivas()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("uuid-estabelecimento", sistemaDelivery.getEstabelecimento().getUuid().toString());
                jsonObject.addProperty("estabelecimento", sistemaDelivery.getEstabelecimento().getNomeEstabelecimento());
                jsonObject.addProperty("usuariosAtivos", sistemaDelivery.getUsuariosAtivos());
                jsonObject.addProperty("estadoWhatsApp", sistemaDelivery.getDriver().getEstadoDriver().name());
                if (sistemaDelivery.getDriver().getEstadoDriver() == EstadoDriver.LOGGED) {
                    jsonObject.addProperty("idWhatsApp", sistemaDelivery.getDriver().getFunctions().getMyChat().getId());
                }
                jsonArray.add(jsonObject);
                if (msgTeste && sistemaDelivery.getDriver().getEstadoDriver() == EstadoDriver.LOGGED) {
                    Chat chat = sistemaDelivery.getDriver().getFunctions().getChatByNumber("5544991050665");
                    chat.sendMessage("Mensagem para verificar se o sistema esta ativo");
                }
            }
            return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(builder.toJson(jsonArray)).build();
        } catch (Exception e) {
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }
}
