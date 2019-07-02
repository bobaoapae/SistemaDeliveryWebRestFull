package restFul;

import adapters.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import modelo.Chat;
import modelo.EstadoDriver;
import org.apache.commons.lang.exception.ExceptionUtils;
import restFul.controle.ControleSessions;
import restFul.controle.ControleTokens;
import restFul.controle.ControleUsuarios;
import restFul.modelo.LoginInUse;
import restFul.modelo.TipoUsuario;
import restFul.modelo.Token;
import restFul.modelo.Usuario;
import sistemaDelivery.SistemaDelivery;
import sistemaDelivery.controle.ControleEstabelecimentos;
import sistemaDelivery.modelo.Estabelecimento;
import sistemaDelivery.modelo.TipoEntrega;
import utils.Utilitarios;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/manager")
public class Manager {

    private Gson builder;

    public Manager() {
        builder = new GsonBuilder().disableHtmlEscaping().
                registerTypeAdapter(LocalTime.class, new DateAdapterSerialize()).
                registerTypeAdapter(LocalTime.class, new DateAdapterDeserialize()).
                registerTypeAdapter(LocalDate.class, new TimestampAdapterSerialize()).
                registerTypeAdapter(LocalDate.class, new TimestampAdapterDeserialize()).
                registerTypeAdapter(Time.class, new TimeAdapter()).
                registerTypeAdapter(Time.class, new TimeAdapterDeserialize()).
                setDateFormat("dd/MM/yyyy HH:mm:ss").
                create();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/reiniciarSistema")
    public Response reiniciarSistema(@QueryParam("securePass") @DefaultValue("") String securePass, @QueryParam("uuid") @DefaultValue("") String uuid) {
        if (!securePass.equals("mkQZUJbvda8NDUAfqUhjc48PQjB5mvV5psxae6uBhvUG4eQcYCfarb9bWC9S3W4HDyaH3CUgqPgeerqr5dYW8ZdhFgUyTCAEvqq8hr5xDqybeUqKwxHjJ2kWKF5vAkz8")) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } else if (uuid.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            try {
                ControleSessions.getInstance().finalizarSessionForEstabelecimento(ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(UUID.fromString(uuid)));
                Thread.sleep(2000);
                ControleSessions.getInstance().getSessionForEstabelecimento(ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(UUID.fromString(uuid)));
                return Response.status(Response.Status.OK).build();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Logger.getLogger("LogGeral").log(Level.SEVERE, e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
            } catch (Exception e) {
                Logger.getLogger("LogGeral").log(Level.SEVERE, e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
            }
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
            Logger.getLogger("LogGeral").log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/login")
    public Response login(@QueryParam("login") String login, @QueryParam("senha") String senha) {
        try {
            Usuario usuario = ControleUsuarios.getInstance().getUsuario(login, senha);
            if (usuario != null) {
                return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(builder.toJson(usuario)).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        } catch (SQLException e) {
            Logger.getLogger("LogGeral").log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/create")
    public Response create(@QueryParam("securePass") @DefaultValue("") String securePass, @QueryParam("login") @DefaultValue("") String login, @QueryParam("senha") @DefaultValue("") String senha, @QueryParam("qtdEstabelecimentos") @DefaultValue("1") int qtdEstabelecimentos) {
        if (!securePass.equals("mkQZUJbvda8NDUAfqUhjc48PQjB5mvV5psxae6uBhvUG4eQcYCfarb9bWC9S3W4HDyaH3CUgqPgeerqr5dYW8ZdhFgUyTCAEvqq8hr5xDqybeUqKwxHjJ2kWKF5vAkz8")) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{message: \"securePass incorreto\"}").build();
        } else if (login.isEmpty() || senha.isEmpty()) {
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
                    return Response.status(Response.Status.CREATED).type(MediaType.APPLICATION_JSON).entity(builder.toJson(usuario)).build();
                } else {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{message: \"Falha ao salvar usuario, verificar log\"}").build();
                }
            } catch (LoginInUse ex) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{message: \"Login já está em uso\"}").build();
            } catch (SQLException e) {
                Logger.getLogger("LogGeral").log(Level.SEVERE, e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{message: \"" + ExceptionUtils.getStackTrace(e) + "\"}").build();
            }
        }
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generateToken")
    public Response generateToken(@QueryParam("login") String login, @QueryParam("senha") String senha, @QueryParam("estabelecimento") String estabelecimento) {
        try {
            Usuario usuario = ControleUsuarios.getInstance().getUsuario(login, senha);
            if (usuario != null) {
                Estabelecimento esta = ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(UUID.fromString(estabelecimento));
                if (esta == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
                if (usuario.getEstabelecimentos().contains(esta)) {
                    Token token = new Token();
                    token.setUsuario(usuario);
                    token.setEstabelecimento(esta);
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_YEAR, 7);
                    token.setValidade(calendar.getTime());
                    try {
                        token.setToken(Utilitarios.generate(128));
                    } catch (NoSuchAlgorithmException e1) {
                        e1.printStackTrace();
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                    }
                    if (!ControleTokens.getInstance().saveToken(token)) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                    }
                    try {
                        ControleSessions.getInstance().getSessionForEstabelecimento(esta);
                        JsonObject object = new JsonObject();
                        object.addProperty("token", token.getToken());
                        NewCookie cookie = new NewCookie("token", token.getToken(), null, null, null, 7 * 24 * 60 * 60, false);
                        return Response.status(Response.Status.CREATED).cookie(cookie).entity(builder.toJson(object)).build();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                    }
                } else {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        } catch (Exception ex) {
            Logger.getLogger("LogGeral").log(Level.SEVERE, ex.getMessage(), ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(ex)).build();
        }
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/criarEstabelecimento")
    public Response criarEstabelecimento(@QueryParam("login") String login, @QueryParam("senha") String senha, @FormParam("estabelecimento") String estabelecimento) {
        try {
            Usuario usuario = ControleUsuarios.getInstance().getUsuario(login, senha);
            if (usuario != null) {
                if (usuario.getTipoUsuario() == TipoUsuario.SUPER_ADMIN || usuario.getEstabelecimentos().size() + 1 <= usuario.getMaxEstabelecimentos()) {
                    Estabelecimento estabelecimento1 = builder.fromJson(estabelecimento, Estabelecimento.class);
                    if (estabelecimento1.getUuid() != null && !usuario.getEstabelecimentos().contains(estabelecimento1)) {
                        return Response.status(Response.Status.BAD_REQUEST).build();
                    }
                    if (estabelecimento1.getTiposEntregas() == null || estabelecimento1.getTiposEntregas().isEmpty()) {
                        estabelecimento1.setTiposEntregas(new ArrayList<>());
                        TipoEntrega tipoEntrega = new TipoEntrega();
                        tipoEntrega.setNome("Retirada");
                        estabelecimento1.getTiposEntregas().add(tipoEntrega);
                        tipoEntrega = new TipoEntrega();
                        tipoEntrega.setNome("Entrega");
                        tipoEntrega.setSolicitarEndereco(true);
                        estabelecimento1.getTiposEntregas().add(tipoEntrega);
                    }
                    if (ControleEstabelecimentos.getInstance().criarEstabelecimento(usuario, estabelecimento1)) {
                        estabelecimento1 = ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(estabelecimento1.getUuid());
                        usuario.getEstabelecimentos().add(estabelecimento1);
                        return Response.status(Response.Status.CREATED).entity(builder.toJson(estabelecimento1)).build();
                    } else {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                    }
                } else {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        } catch (SQLException e) {
            Logger.getLogger("LogGeral").log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirEstabelecimento")
    public Response excluirEstabelecimento(@QueryParam("login") String login, @QueryParam("senha") String senha, @QueryParam("uuid") String uuid) {
        try {
            Usuario usuario = ControleUsuarios.getInstance().getUsuario(login, senha);
            if (usuario != null) {
                Estabelecimento estabelecimento1 = ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(UUID.fromString(uuid));
                if (!usuario.getEstabelecimentos().contains(estabelecimento1)) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
                if (ControleEstabelecimentos.getInstance().excluirEstabelecimento(estabelecimento1)) {
                    synchronized (usuario.getEstabelecimentos()) {
                        usuario.getEstabelecimentos().remove(estabelecimento1);
                    }
                    return Response.status(Response.Status.OK).build();
                } else {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                }
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        } catch (SQLException | IOException e) {
            Logger.getLogger("LogGeral").log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

}
