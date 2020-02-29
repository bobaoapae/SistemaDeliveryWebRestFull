package restFul;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import modelo.DriverState;
import org.apache.commons.lang.exception.ExceptionUtils;
import restFul.controle.ControleSessions;
import restFul.controle.ControleSistema;
import restFul.controle.ControleUsuarios;
import restFul.modelo.LoginInUse;
import restFul.modelo.TipoUsuario;
import restFul.modelo.Usuario;
import sistemaDelivery.SistemaDelivery;
import sistemaDelivery.controle.ControleEstabelecimentos;
import sistemaDelivery.modelo.Estabelecimento;
import utils.Utilitarios;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@Path("/adm")
public class Adm {

    private JsonParser parser;
    private Gson builder;


    public Adm() {
        parser = new JsonParser();
        builder = Utilitarios.getDefaultGsonBuilder(null).create();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/ativarUsuario")
    public Response ativarUsuario(@QueryParam("uuid") String uuid) {
        try {
            Usuario usuario = ControleUsuarios.getInstance().getUsuarioByUUID(UUID.fromString(uuid));
            usuario.setAtivo(true);
            if (ControleUsuarios.getInstance().salvarUsuario(usuario)) {
                return Response.status(Response.Status.CREATED).type(MediaType.APPLICATION_JSON).entity(builder.toJson(usuario)).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{message: \"Falha ao salvar usuario, verificar log\"}").build();
            }
        } catch (Exception e) {
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{message: \"" + ExceptionUtils.getStackTrace(e) + "\"}").build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/desativarUsuario")
    public Response desativarUsuario(@QueryParam("uuid") String uuid) {
        try {
            Usuario usuario = ControleUsuarios.getInstance().getUsuarioByUUID(UUID.fromString(uuid));
            usuario.setAtivo(false);
            if (ControleUsuarios.getInstance().salvarUsuario(usuario)) {
                for (Estabelecimento estabelecimento : usuario.getEstabelecimentos()) {
                    ControleSessions.getInstance().finalizarSessionForEstabelecimento(estabelecimento);
                }
                return Response.status(Response.Status.CREATED).type(MediaType.APPLICATION_JSON).entity(builder.toJson(usuario)).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{message: \"Falha ao salvar usuario, verificar log\"}").build();
            }
        } catch (Exception e) {
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{message: \"" + ExceptionUtils.getStackTrace(e) + "\"}").build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/create")
    public Response create(@QueryParam("login") @DefaultValue("") String login, @QueryParam("senha") @DefaultValue("") String senha, @QueryParam("qtdEstabelecimentos") @DefaultValue("1") int qtdEstabelecimentos) {
        if (login.isEmpty() || senha.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{message: \"Campos obrigatórios faltando\"}").build();
        } else {
            if (qtdEstabelecimentos < 1) {
                qtdEstabelecimentos = 1;
            }
            Usuario usuario = new Usuario();
            usuario.setMaxEstabelecimentos(qtdEstabelecimentos);
            usuario.setUsuario(login);
            usuario.setSenha(senha);
            usuario.setTipoUsuario(TipoUsuario.ADMIN);
            try {
                if (ControleUsuarios.getInstance().salvarUsuario(usuario)) {
                    return Response.status(Response.Status.CREATED).type(MediaType.APPLICATION_JSON).entity(builder.toJson(ControleUsuarios.getInstance().getUsuarioByUUID(usuario.getUuid()))).build();
                } else {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{message: \"Falha ao salvar usuario, verificar log\"}").build();
                }
            } catch (LoginInUse ex) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{message: \"Login já está em uso\"}").build();
            } catch (SQLException e) {
                ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{message: \"" + ExceptionUtils.getStackTrace(e) + "\"}").build();
            }
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/reiniciarSistema")
    public Response reiniciarSistema(@QueryParam("uuid") @DefaultValue("") String uuid) {
        new Thread(() -> {
            try {
                ControleSessions.getInstance().finalizarSessionForEstabelecimento(ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(UUID.fromString(uuid)));
                Thread.sleep(2000);
                ControleSessions.getInstance().getSessionForEstabelecimento(ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(UUID.fromString(uuid)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
            } catch (Exception e) {
                ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        }).start();
        return Response.status(Response.Status.OK).build();
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
                jsonObject.addProperty("estadoWhatsApp", sistemaDelivery.getDriver().getDriverState().name());
                if (sistemaDelivery.getDriver().getDriverState() == DriverState.LOGGED) {
                    jsonObject.addProperty("idWhatsApp", sistemaDelivery.getDriver().getFunctions().getMyChat().join().getId());
                }
                jsonArray.add(jsonObject);
                if (msgTeste && sistemaDelivery.getDriver().getDriverState() == DriverState.LOGGED) {
                    sistemaDelivery.getDriver().getFunctions().getChatByNumber("5544991050665").thenCompose(chat -> {
                        if (chat != null) {
                            return chat.sendMessage("Mensagem para verificar se o sistema esta ativo");
                        } else {
                            return CompletableFuture.failedFuture(new RuntimeException("Chat Não Encontrado"));
                        }
                    });
                }
            }
            return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(builder.toJson(jsonArray)).build();
        } catch (Exception e) {
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }
}
