package restFul;

import adapters.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.lang.exception.ExceptionUtils;
import restFul.controle.ControleSessions;
import restFul.controle.ControleSistema;
import restFul.controle.ControleTokens;
import restFul.controle.ControleUsuarios;
import restFul.modelo.TipoUsuario;
import restFul.modelo.Token;
import restFul.modelo.Usuario;
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
    @Path("/login")
    public Response login(@QueryParam("login") String login, @QueryParam("senha") String senha) {
        try {
            Usuario usuario = ControleUsuarios.getInstance().getUsuario(login, senha, true);
            if (usuario != null) {
                return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(builder.toJson(usuario)).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        } catch (SQLException e) {
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generateToken")
    public Response generateToken(@QueryParam("login") String login, @QueryParam("senha") String senha, @QueryParam("estabelecimento") String estabelecimento) {
        try {
            Usuario usuario = ControleUsuarios.getInstance().getUsuario(login, senha, true);
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
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(ex)).build();
        }
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/criarEstabelecimento")
    public Response criarEstabelecimento(@QueryParam("login") String login, @QueryParam("senha") String senha, @FormParam("estabelecimento") String estabelecimento) {
        try {
            Usuario usuario = ControleUsuarios.getInstance().getUsuario(login, senha, true);
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
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirEstabelecimento")
    public Response excluirEstabelecimento(@QueryParam("login") String login, @QueryParam("senha") String senha, @QueryParam("uuid") String uuid) {
        try {
            Usuario usuario = ControleUsuarios.getInstance().getUsuario(login, senha, true);
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
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/atualizarEstabelecimentos")
    public Response atualizarEstabelecimentos(@QueryParam("login") String login, @QueryParam("senha") String senha) {
        try {
            Usuario usuario = ControleUsuarios.getInstance().getUsuario(login, senha, true);
            if (usuario != null) {
                ControleUsuarios.getInstance().atualizarEstabelecimentosUsuario(usuario);
                return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(builder.toJson(usuario)).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        } catch (SQLException e) {
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

}
