package restFul;

import adapters.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import restFul.controle.ControleSessions;
import restFul.controle.ControleTokens;
import restFul.controle.ControleUsuarios;
import restFul.modelo.Token;
import restFul.modelo.Usuario;
import sistemaDelivery.controle.ControleEstabelecimentos;
import sistemaDelivery.modelo.Estabelecimento;
import utils.Utilitarios;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.UUID;

@Path("/manager")
public class Manager {

    private Gson builder;

    public Manager() {
        builder = new GsonBuilder().disableHtmlEscaping().
                registerTypeAdapter(LocalTime.class, new LocalTimeAdapter()).
                registerTypeAdapter(LocalTime.class, new LocalTimeAdapterDeserialize()).
                registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).
                registerTypeAdapter(LocalDate.class, new LocalDateAdapterDeserialize()).
                registerTypeAdapter(Time.class, new TimeAdapter()).
                registerTypeAdapter(Time.class, new TimeAdapterDeserialize()).
                setDateFormat("dd/MM/yyyy HH:mm:ss").
                create();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/login")
    public Response login(@QueryParam("login") String login, @QueryParam("senha") String senha) {
        Usuario usuario = ControleUsuarios.getInstace().getUsuario(login, senha);
        if (usuario != null) {
            return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(builder.toJson(usuario)).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generateToken")
    public Response generateToken(@QueryParam("login") String login, @QueryParam("senha") String senha, @QueryParam("estabelecimento") String estabelecimento) {
        try {
            Usuario usuario = ControleUsuarios.getInstace().getUsuario(login, senha);
            if (usuario != null) {
                Estabelecimento esta = ControleEstabelecimentos.getInstace().getEstabelecimentoByUUID(UUID.fromString(estabelecimento));
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
                    if (!ControleTokens.getInstace().saveToken(token)) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                    }
                    try {
                        ControleSessions.getInstance().getSessionForEstabelecimento(esta);
                        JsonObject object = new JsonObject();
                        object.addProperty("token", token.getToken());
                        NewCookie cookie = new NewCookie("token", token.getToken(), "/", "", "", 7 * 24 * 60 * 60, false);
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
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/criarEstabelecimento")
    public Response criarEstabelecimento(@QueryParam("login") String login, @QueryParam("senha") String senha, @FormParam("estabelecimento") String estabelecimento) {
        Usuario usuario = ControleUsuarios.getInstace().getUsuario(login, senha);
        if (usuario != null) {
            Estabelecimento estabelecimento1 = builder.fromJson(estabelecimento, Estabelecimento.class);
            if (estabelecimento1.getUuid() != null && !usuario.getEstabelecimentos().contains(estabelecimento1)) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (ControleEstabelecimentos.getInstace().criarEstabelecimento(usuario, estabelecimento1)) {
                estabelecimento1 = ControleEstabelecimentos.getInstace().getEstabelecimentoByUUID(estabelecimento1.getUuid());
                usuario.getEstabelecimentos().add(estabelecimento1);
                return Response.status(Response.Status.CREATED).entity(builder.toJson(estabelecimento1)).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/excluirEstabelecimento")
    public Response excluirEstabelecimento(@QueryParam("login") String login, @QueryParam("senha") String senha, @QueryParam("uuid") String uuid) {
        Usuario usuario = ControleUsuarios.getInstace().getUsuario(login, senha);
        if (usuario != null) {
            Estabelecimento estabelecimento1 = ControleEstabelecimentos.getInstace().getEstabelecimentoByUUID(UUID.fromString(uuid));
            if (!usuario.getEstabelecimentos().contains(estabelecimento1)) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            estabelecimento1.setAtivo(false);
            if (ControleEstabelecimentos.getInstace().salvarEstabelecimento(estabelecimento1)) {
                usuario.getEstabelecimentos().remove(estabelecimento1);
                return Response.status(Response.Status.CREATED).entity(builder.toJson(estabelecimento1)).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

}
