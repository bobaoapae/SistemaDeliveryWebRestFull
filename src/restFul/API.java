package restFul;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import driver.WebWhatsDriver;
import modelo.*;
import org.apache.commons.lang.exception.ExceptionUtils;
import restFul.modelo.Token;
import sistemaDelivery.SistemaDelivery;
import sistemaDelivery.controle.*;
import sistemaDelivery.modelo.*;
import utils.Utilitarios;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Path("/api")
public class API {

    private Gson builder;
    private Token token;
    private List<Pedido> pedidosSendoCriados;
    JsonParser parser;


    public API(@Context HttpServletRequest session, @Context SecurityContext securityContext) {
        this.token = (Token) securityContext.getUserPrincipal();
        builder = Utilitarios.getDefaultGsonBuilder(null).create();
        if (session.getSession(true).getAttribute("pedidosSendoCriados") == null) {
            session.getSession().setAttribute("pedidosSendoCriados", Collections.synchronizedList(new ArrayList<>()));
        }
        pedidosSendoCriados = ((List<Pedido>) session.getSession().getAttribute("pedidosSendoCriados"));
        parser = new JsonParser();
    }

    @GET
    @Path("/eventos")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void eventoNovoPedido(@Context SseEventSink sink, @Context Sse sse) {
        SistemaDelivery sistemaDelivery = token.getSistemaDelivery();
        if (sistemaDelivery.getBroadcaster() != null) {
            sistemaDelivery.getBroadcaster().register(sink);
        } else {
            sistemaDelivery.setSse(sse);
            sistemaDelivery.setBroadcaster(sse.newBroadcaster());
            sistemaDelivery.getBroadcaster().register(sink);
        }
        sink.send(sse.newEvent("ok"));
    }

    @GET
    @Path("/eventosWpp")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void eventosWpp(@Context SseEventSink sink, @Context Sse sse) {
        SistemaDelivery sistemaDelivery = token.getSistemaDelivery();
        if (sistemaDelivery.getBroadcasterWhats() != null) {
            sistemaDelivery.getBroadcasterWhats().register(sink);
        } else {
            sistemaDelivery.setSseWhats(sse);
            sistemaDelivery.setBroadcasterWhats(sse.newBroadcaster());
            sistemaDelivery.getBroadcasterWhats().register(sink);
        }
        sink.send(sse.newEvent("ok"));
    }

    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInfo() {
        return Response.status(Response.Status.OK).entity(builder.toJson(token.getEstabelecimento())).build();
    }

    @GET
    @Path("/estadoWhats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response estadoWhats() {
        JsonObject object = new JsonObject();
        try {
            WebWhatsDriver driver = token.getSistemaDelivery().getDriver();
            object.addProperty("status", driver.getEstadoDriver().toString());
            if (driver.getEstadoDriver() == EstadoDriver.WAITING_QR_CODE_SCAN) {
                object.addProperty("qrCode", driver.getQrCodePlain());
            }
            return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(builder.toJson(object)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/tempoMedio")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tempoMedio(@QueryParam("entrega") String entrega, @QueryParam("retirada") String retirada) {
        try {
            token.getEstabelecimento().setTempoMedioEntrega(Integer.parseInt(entrega));
            token.getEstabelecimento().setTempoMedioRetirada(Integer.parseInt(retirada));
            if (ControleEstabelecimentos.getInstance().salvarEstabelecimento(token.getEstabelecimento())) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(token.getEstabelecimento())).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @POST
    @Path("/alterarEstabelecimento")
    @Produces(MediaType.APPLICATION_JSON)
    public Response alterarEstabelecimento(@FormParam("estabelecimento") String estabelecimento) {
        try {
            Estabelecimento novosValoresEstabelecimento = builder.fromJson(estabelecimento, Estabelecimento.class);
            token.getEstabelecimento().setAbrirFecharPedidosAutomatico(novosValoresEstabelecimento.isAbrirFecharPedidosAutomatico());
            token.getEstabelecimento().setAgendamentoDePedidos(novosValoresEstabelecimento.isAgendamentoDePedidos());
            token.getEstabelecimento().setHoraInicioReservas(novosValoresEstabelecimento.getHoraInicioReservas());
            token.getEstabelecimento().setNomeBot(novosValoresEstabelecimento.getNomeBot());
            token.getEstabelecimento().setNomeEstabelecimento(novosValoresEstabelecimento.getNomeEstabelecimento());
            token.getEstabelecimento().setNumeroAviso(novosValoresEstabelecimento.getNumeroAviso());
            token.getEstabelecimento().setReservas(novosValoresEstabelecimento.isReservas());
            token.getEstabelecimento().setReservasComPedidosFechados(novosValoresEstabelecimento.isReservasComPedidosFechados());
            token.getEstabelecimento().setWebHookNovoPedido(novosValoresEstabelecimento.getWebHookNovoPedido());
            token.getEstabelecimento().setWebHookNovaReserva(novosValoresEstabelecimento.getWebHookNovaReserva());
            token.getEstabelecimento().setLogo(novosValoresEstabelecimento.getLogo());
            token.getEstabelecimento().setValidadeSeloFidelidade(novosValoresEstabelecimento.getValidadeSeloFidelidade());
            token.getEstabelecimento().setValorSelo(novosValoresEstabelecimento.getValorSelo());
            token.getEstabelecimento().setMaximoSeloPorCompra(novosValoresEstabelecimento.getMaximoSeloPorCompra());
            token.getEstabelecimento().setTimeZone(novosValoresEstabelecimento.getTimeZoneObject().toZoneId().getDisplayName(TextStyle.NARROW, Locale.forLanguageTag("pt-BR")));
            if (ControleEstabelecimentos.getInstance().salvarEstabelecimento(token.getEstabelecimento())) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(token.getEstabelecimento())).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }


    @GET
    @Path("/categorias")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCategorias(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                List<Categoria> categorias = ControleCategorias.getInstance().getCategoriasEstabelecimento(token.getEstabelecimento());
                String json = builder.toJson(categorias);
                return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(json).build();
            } else {
                Categoria categoria = ControleCategorias.getInstance().getCategoriaByUUID(UUID.fromString(uuid));
                if (categoria == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                } else if (categoria.getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.OK).entity(builder.toJson(categoria)).build();
                } else {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/gruposAdicionaisCategoria")
    @Produces(MediaType.APPLICATION_JSON)
    public Response gruposAdicionaisCategoria(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            } else {
                Categoria categoria = ControleCategorias.getInstance().getCategoriaByUUID(UUID.fromString(uuid));
                if (categoria == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                } else if (categoria.getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.OK).entity(builder.toJson(categoria.getGruposAdicionais())).build();
                } else {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarCategoria")
    public Response salvarCategoria(@FormParam("categoria") String cat) {
        try {
            Categoria categoria = builder.fromJson(cat, Categoria.class);
            categoria.setEstabelecimento(token.getEstabelecimento());
            categoria.setCategoriaPai(ControleCategorias.getInstance().getCategoriaByUUID(categoria.getUuid_categoria_pai()));
            categoria.setAtivo(true);
            for (UUID uuidCat : categoria.getUuidsCategoriasNecessarias()) {
                Categoria catNecessaria = ControleCategorias.getInstance().getCategoriaByUUID(uuidCat);
                if (catNecessaria == null) {
                    return Response.status(Response.Status.BAD_REQUEST).entity(uuidCat + " - uuid invalido").build();
                }
                if (!catNecessaria.getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
                categoria.getCategoriasNecessarias().add(catNecessaria);
            }
            if (categoria.getCategoriaPai() != null && !categoria.getCategoriaPai().getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (ControleCategorias.getInstance().salvarCategoria(categoria)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleCategorias.getInstance().getCategoriaByUUID(categoria.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/alterarVisibilidadeCategoria")
    public Response alterarVisibilidadeCategoria(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Categoria categoria = ControleCategorias.getInstance().getCategoriaByUUID(UUID.fromString(uuid));
            if (categoria == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!categoria.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            categoria.setVisivel(!categoria.isVisivel());
            if (ControleCategorias.getInstance().salvarCategoria(categoria)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleCategorias.getInstance().getCategoriaByUUID(categoria.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/alterarEntregaGratisCategoria")
    public Response alterarEntregaGratisCategoria(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Categoria categoria = ControleCategorias.getInstance().getCategoriaByUUID(UUID.fromString(uuid));
            if (categoria == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!categoria.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            categoria.setEntregaGratis(!categoria.isEntregaGratis());
            if (ControleCategorias.getInstance().salvarCategoria(categoria)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleCategorias.getInstance().getCategoriaByUUID(categoria.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirCategoria")
    public Response excluirCategoria(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Categoria categoria = ControleCategorias.getInstance().getCategoriaByUUID(UUID.fromString(uuid));
            if (categoria == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!categoria.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (ControleCategorias.getInstance().excluirCategoria(categoria)) {
                return Response.status(Response.Status.CREATED).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarProduto")
    public Response salvarProduto(@FormParam("produto") String prod) {
        try {
            Produto produto = builder.fromJson(prod, Produto.class);
            produto.setCategoria(ControleCategorias.getInstance().getCategoriaByUUID(produto.getUuid_categoria()));
            produto.setAtivo(true);
            if (!produto.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (ControleProdutos.getInstance().salvarProduto(produto)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleProdutos.getInstance().getProdutoByUUID(produto.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/produtos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProdutos(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.OK).entity(builder.toJson(ControleProdutos.getInstance().getProdutosEstabelecimento(token.getEstabelecimento()))).build();
            } else {
                Categoria categoria = ControleCategorias.getInstance().getCategoriaByUUID(UUID.fromString(uuid));
                if (categoria == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                } else if (categoria.getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.OK).entity(builder.toJson(categoria.getProdutos())).build();
                } else {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/produto")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProduto(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            } else {
                Produto produto = ControleProdutos.getInstance().getProdutoByUUID(UUID.fromString(uuid));
                if (produto == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                } else if (produto.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.OK).entity(builder.toJson(produto)).build();
                } else {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/gruposAdicionaisProduto")
    @Produces(MediaType.APPLICATION_JSON)
    public Response gruposAdicionaisProduto(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            } else {
                Produto produto = ControleProdutos.getInstance().getProdutoByUUID(UUID.fromString(uuid));
                if (produto == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                } else if (produto.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.OK).entity(builder.toJson(produto.getGruposAdicionais())).build();
                } else {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/alterarVisibilidadeProduto")
    public Response alterarVisibilidadeProduto(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Produto produto = ControleProdutos.getInstance().getProdutoByUUID(UUID.fromString(uuid));
            if (produto == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!produto.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            produto.setVisivel(!produto.isVisivel());
            if (ControleProdutos.getInstance().salvarProduto(produto)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleProdutos.getInstance().getProdutoByUUID(produto.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirProduto")
    public Response excluirProduto(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Produto produto = ControleProdutos.getInstance().getProdutoByUUID(UUID.fromString(uuid));
            if (produto == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!produto.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (ControleProdutos.getInstance().excluirProduto(produto)) {
                return Response.status(Response.Status.CREATED).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/rodizios")
    public Response getRodizios(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.OK).entity(builder.toJson(token.getEstabelecimento().getRodizios())).build();
            } else {
                Rodizio rodizio = ControleRodizios.getInstace().getRodizioByUUID(UUID.fromString(uuid));
                if (rodizio == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
                if (!rodizio.getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
                return Response.status(Response.Status.OK).entity(builder.toJson(rodizio)).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarRodizio")
    public Response salvarRodizio(@FormParam("rodizio") String rod) {
        try {
            Rodizio rodizio = builder.fromJson(rod, Rodizio.class);
            rodizio.setEstabelecimento(token.getEstabelecimento());
            if (ControleRodizios.getInstace().salvarRodizio(rodizio)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleRodizios.getInstace().getRodizioByUUID(rodizio.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirRodizio")
    public Response excluirRodizio(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Rodizio rodizio = ControleRodizios.getInstace().getRodizioByUUID(UUID.fromString(uuid));
            if (rodizio == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!rodizio.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (ControleRodizios.getInstace().excluirRodizio(rodizio)) {
                return Response.status(Response.Status.CREATED).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarGrupoAdicional")
    public Response salvarGrupoAdicional(@FormParam("grupo") String grupo) {
        try {
            GrupoAdicional grupoAdicional = builder.fromJson(grupo, GrupoAdicional.class);
            if (grupoAdicional.getUuid_categoria() != null) {
                grupoAdicional.setCategoria(ControleCategorias.getInstance().getCategoriaByUUID(grupoAdicional.getUuid_categoria()));
                if (!grupoAdicional.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            } else if (grupoAdicional.getUuid_produto() != null) {
                grupoAdicional.setProduto(ControleProdutos.getInstance().getProdutoByUUID(grupoAdicional.getUuid_produto()));
                if (!grupoAdicional.getProduto().getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("uuid categoria ou produto faltando").build();
            }
            if (ControleGruposAdicionais.getInstance().salvarGrupoAdicional(grupoAdicional)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleGruposAdicionais.getInstance().getGrupoByUUID(grupoAdicional.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/grupoAdicionais")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGrupoAdicionais(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            } else {
                GrupoAdicional grupoAdicional = ControleGruposAdicionais.getInstance().getGrupoByUUID(UUID.fromString(uuid));
                if (grupoAdicional == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
                if (grupoAdicional.getCategoria() != null) {
                    if (grupoAdicional.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                        return Response.status(Response.Status.OK).entity(builder.toJson(grupoAdicional)).build();
                    } else {
                        return Response.status(Response.Status.BAD_REQUEST).build();
                    }
                } else {
                    if (grupoAdicional.getProduto().getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                        return Response.status(Response.Status.OK).entity(builder.toJson(grupoAdicional)).build();
                    } else {
                        return Response.status(Response.Status.BAD_REQUEST).build();
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirGrupoAdicional")
    public Response excluirGrupoAdicional(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            GrupoAdicional grupoAdicional = ControleGruposAdicionais.getInstance().getGrupoByUUID(UUID.fromString(uuid));
            if (grupoAdicional == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (grupoAdicional.getCategoria() != null) {
                if (!grupoAdicional.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            } else if (grupoAdicional.getProduto() != null) {
                if (!grupoAdicional.getProduto().getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }
            if (ControleGruposAdicionais.getInstance().excluirGrupo(grupoAdicional)) {
                return Response.status(Response.Status.CREATED).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/exportarGrupoAdicional")
    public Response exportarGrupoAdicional(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            GrupoAdicional grupoAdicional = ControleGruposAdicionais.getInstance().getGrupoByUUID(UUID.fromString(uuid));
            if (grupoAdicional == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (grupoAdicional.getCategoria() != null) {
                if (!grupoAdicional.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            } else if (grupoAdicional.getProduto() != null) {
                if (!grupoAdicional.getProduto().getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }
            String exportacao = "Nome Grupo; Qtd Min; Qtd Max; Forma Cobrança\r\n" + grupoAdicional.getNomeGrupo() + ";" + grupoAdicional.getQtdMin() + ";" + grupoAdicional.getQtdMax() + ";" + grupoAdicional.getFormaCobranca().toString() + "\r\nNome;Descrição;Valor\r\n";
            for (int linha = 0; linha < grupoAdicional.getAdicionais().size(); linha++) {
                exportacao += grupoAdicional.getAdicionais().get(linha).getNome() + ";";
                exportacao += grupoAdicional.getAdicionais().get(linha).getDescricao() + ";";
                exportacao += grupoAdicional.getAdicionais().get(linha).getValor() + ";\r\n";
            }
            return Response.status(Response.Status.OK).entity(Base64.getEncoder().encodeToString(exportacao.getBytes(Charset.forName("UTF-8")))).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/importarGrupoAdicionalCategoria")
    public Response importarGrupoCategoria(@QueryParam("uuid") String uuid, @FormParam("arquivo") String arquivo) {
        try {
            String decoded = new String(Base64.getDecoder().decode(arquivo), Charset.forName("UTF-8"));
            decoded = decoded.replaceAll("\t", ";").replace("\uFEFF", "");
            ;
            String linhas[] = decoded.split("\r\n");
            GrupoAdicional grupoAdicional = new GrupoAdicional();
            grupoAdicional.setNomeGrupo(linhas[1].split(";")[0]);
            grupoAdicional.setQtdMin(Integer.parseInt(linhas[1].split(";")[1]));
            grupoAdicional.setQtdMax(Integer.parseInt(linhas[1].split(";")[2]));
            grupoAdicional.setFormaCobranca(GrupoAdicional.FormaCobranca.valueOf(linhas[1].split(";")[3]));
            List<AdicionalProduto> adicionalProdutos = new ArrayList<>();
            for (int x = 3; x < linhas.length; x++) {
                String linhaAtual[] = linhas[x].split(";");
                AdicionalProduto adicionalProduto = new AdicionalProduto();
                adicionalProduto.setNome(linhaAtual[0]);
                adicionalProduto.setDescricao(linhaAtual[1]);
                adicionalProduto.setValor(Double.parseDouble(linhaAtual[2]));
                adicionalProdutos.add(adicionalProduto);
            }
            Categoria categoria = ControleCategorias.getInstance().getCategoriaByUUID(UUID.fromString(uuid));
            if (categoria == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!categoria.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            grupoAdicional.setCategoria(categoria);
            grupoAdicional.setUuid_categoria(categoria.getUuid());
            if (ControleGruposAdicionais.getInstance().salvarGrupoAdicional(grupoAdicional)) {
                for (AdicionalProduto adicionalProduto : adicionalProdutos) {
                    adicionalProduto.setGrupoAdicional(ControleGruposAdicionais.getInstance().getGrupoByUUID(grupoAdicional.getUuid()));
                    if (!ControleAdicionais.getInstance().salvarAdicional(adicionalProduto)) {
                        ControleGruposAdicionais.getInstance().excluirGrupo(grupoAdicional);
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                    }
                }
                return Response.status(Response.Status.OK).entity(builder.toJson(ControleGruposAdicionais.getInstance().getGrupoByUUID(grupoAdicional.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/importarGrupoAdicionalProduto")
    public Response importarGrupoProduto(@QueryParam("uuid") String uuid, @FormParam("arquivo") String arquivo) {
        try {
            String decoded = new String(Base64.getDecoder().decode(arquivo), Charset.forName("UTF-8"));
            decoded = decoded.replaceAll("\t", ";").replace("\uFEFF", "");
            ;
            String linhas[] = decoded.split("\r\n");
            GrupoAdicional grupoAdicional = new GrupoAdicional();
            grupoAdicional.setNomeGrupo(linhas[1].split(";")[0]);
            grupoAdicional.setQtdMin(Integer.parseInt(linhas[1].split(";")[1]));
            grupoAdicional.setQtdMax(Integer.parseInt(linhas[1].split(";")[2]));
            grupoAdicional.setFormaCobranca(GrupoAdicional.FormaCobranca.valueOf(linhas[1].split(";")[3]));
            List<AdicionalProduto> adicionalProdutos = new ArrayList<>();
            for (int x = 3; x < linhas.length; x++) {
                String linhaAtual[] = linhas[x].split(";");
                AdicionalProduto adicionalProduto = new AdicionalProduto();
                adicionalProduto.setNome(linhaAtual[0]);
                adicionalProduto.setDescricao(linhaAtual[1]);
                adicionalProduto.setValor(Double.parseDouble(linhaAtual[2]));
                adicionalProdutos.add(adicionalProduto);
            }
            Produto produto = ControleProdutos.getInstance().getProdutoByUUID(UUID.fromString(uuid));
            if (produto == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!produto.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            grupoAdicional.setProduto(produto);
            grupoAdicional.setUuid_produto(produto.getUuid());
            if (ControleGruposAdicionais.getInstance().salvarGrupoAdicional(grupoAdicional)) {
                for (AdicionalProduto adicionalProduto : adicionalProdutos) {
                    adicionalProduto.setGrupoAdicional(ControleGruposAdicionais.getInstance().getGrupoByUUID(grupoAdicional.getUuid()));
                    if (!ControleAdicionais.getInstance().salvarAdicional(adicionalProduto)) {
                        ControleGruposAdicionais.getInstance().excluirGrupo(grupoAdicional);
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                    }
                }
                return Response.status(Response.Status.OK).entity(builder.toJson(ControleGruposAdicionais.getInstance().getGrupoByUUID(grupoAdicional.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarAdicional")
    public Response salvarAdicional(@FormParam("adicional") String adicional) {
        try {
            AdicionalProduto adicionalProduto = builder.fromJson(adicional, AdicionalProduto.class);
            GrupoAdicional grupoAdicional = null;
            if (adicionalProduto.getUuid() != null) {
                grupoAdicional = ControleAdicionais.getInstance().getAdicionalByUUID(adicionalProduto.getUuid()).getGrupoAdicional();
            } else {
                grupoAdicional = ControleGruposAdicionais.getInstance().getGrupoByUUID(adicionalProduto.getUuid_grupo_adicional());
            }
            if (grupoAdicional.getCategoria() != null) {
                if (!grupoAdicional.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            } else if (!grupoAdicional.getProduto().getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            adicionalProduto.setGrupoAdicional(grupoAdicional);
            if (ControleAdicionais.getInstance().salvarAdicional(adicionalProduto)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleAdicionais.getInstance().getAdicionalByUUID(adicionalProduto.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/adicional")
    @Produces(MediaType.APPLICATION_JSON)
    public Response adicional(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            } else {
                AdicionalProduto adicional = ControleAdicionais.getInstance().getAdicionalByUUID(UUID.fromString(uuid));
                if (adicional == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
                if (adicional.getGrupoAdicional().getCategoria() != null) {
                    if (adicional.getGrupoAdicional().getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                        return Response.status(Response.Status.OK).entity(builder.toJson(adicional)).build();
                    } else {
                        return Response.status(Response.Status.BAD_REQUEST).build();
                    }
                } else {
                    if (adicional.getGrupoAdicional().getProduto().getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                        return Response.status(Response.Status.OK).entity(builder.toJson(adicional)).build();
                    } else {
                        return Response.status(Response.Status.BAD_REQUEST).build();
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirAdicional")
    public Response excluirAdicional(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            AdicionalProduto adicionalProduto = ControleAdicionais.getInstance().getAdicionalByUUID(UUID.fromString(uuid));
            if (adicionalProduto == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            GrupoAdicional grupoAdicional = adicionalProduto.getGrupoAdicional();
            if (grupoAdicional.getCategoria() != null) {
                if (!grupoAdicional.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            } else if (grupoAdicional.getProduto() != null) {
                if (!grupoAdicional.getProduto().getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }
            if (ControleAdicionais.getInstance().excluirAdicional(adicionalProduto)) {
                return Response.status(Response.Status.CREATED).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/reservaImpressa")
    @Produces(MediaType.TEXT_PLAIN)
    public Response reservaImpressa(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Reserva reserva = ControleReservas.getInstance().getReservaByUUID(UUID.fromString(uuid));
            if (reserva == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!reserva.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            reserva.setImpresso(true);
            if (ControleReservas.getInstance().salvarReserva(reserva)) {
                return Response.status(Response.Status.CREATED).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/reservasImprimir")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reservasImprimir() {
        try {
            return Response.status(Response.Status.OK).entity(builder.toJson(ControleReservas.getInstance().getReservasImprimir(token.getEstabelecimento()))).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/reservas")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReservas(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.OK).entity(builder.toJson(ControleReservas.getInstance().getReservasEstabelecimento(token.getEstabelecimento()))).build();
            } else {
                Reserva reserva = ControleReservas.getInstance().getReservaByUUID(UUID.fromString(uuid));
                if (reserva == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                } else if (reserva.getEstabelecimento().equals(token.getEstabelecimento())) {
                    return Response.status(Response.Status.OK).entity(builder.toJson(reserva)).build();
                } else {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarReserva")
    public Response salvarReserva(@FormParam("reserva") String res) {
        try {
            Reserva reserva = builder.fromJson(res, Reserva.class);
            reserva.setEstabelecimento(token.getEstabelecimento());
            reserva.setCliente(ControleClientes.getInstance().getClienteByUUID(reserva.getUuid_cliente()));
            if (ControleReservas.getInstance().salvarReserva(reserva)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleReservas.getInstance().getReservaByUUID(reserva.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/excluirReserva")
    public Response excluirReserva(@QueryParam("uuid") String uuid) {
        try {
            Reserva reserva = ControleReservas.getInstance().getReservaByUUID(UUID.fromString(uuid));
            if (!reserva.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (ControleReservas.getInstance().excluirReserva(reserva)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleReservas.getInstance().getReservaByUUID(reserva.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/tiposEntregas")
    public Response tiposEntregas() {
        return Response.status(Response.Status.CREATED).entity(builder.toJson(token.getEstabelecimento().getTiposEntregas())).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarTipoEntrega")
    public Response salvarTipoEntrega(@FormParam("tipoEntrega") String tipo) {
        try {
            TipoEntrega tipoEntrega = builder.fromJson(tipo, TipoEntrega.class);
            tipoEntrega.setEstabelecimento(token.getEstabelecimento());
            if (ControleTiposEntrega.getInstance().salvarTipoEntrega(tipoEntrega)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleTiposEntrega.getInstance().getTipoEntregaByUUID(tipoEntrega.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/excluirTipoEntrega")
    public Response excluirTipoEntrega(@QueryParam("uuid") String uuid) {
        try {
            TipoEntrega tipoEntrega = ControleTiposEntrega.getInstance().getTipoEntregaByUUID(UUID.fromString(uuid));
            if (!tipoEntrega.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (ControleTiposEntrega.getInstance().excluirTipoEntrega(tipoEntrega)) {
                return Response.status(Response.Status.CREATED).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/horariosFuncionamento")
    public Response horariosFuncionamento() {
        List<HorarioFuncionamento> horarios = new ArrayList<>();
        for (Map.Entry<DayOfWeek, List<HorarioFuncionamento>> entry : token.getEstabelecimento().getHorariosFuncionamento().entrySet()) {
            for (HorarioFuncionamento horarioFuncionamento : entry.getValue()) {
                horarios.add(horarioFuncionamento);
            }
        }
        return Response.status(Response.Status.CREATED).entity(builder.toJson(horarios)).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarHorarioFuncionamento")
    public Response salvarHorarioFuncionamento(@FormParam("horarioFuncionamento") String horario) {
        try {
            HorarioFuncionamento horarioFuncionamento = builder.fromJson(horario, HorarioFuncionamento.class);
            horarioFuncionamento.setEstabelecimento(token.getEstabelecimento());
            if (ControleHorariosFuncionamento.getInstance().salvarHorarioFuncionamento(horarioFuncionamento)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleHorariosFuncionamento.getInstance().getHorarioFuncionamentoByUUID(horarioFuncionamento.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/excluirHorarioFuncionamento")
    public Response excluirHorarioFuncionamento(@QueryParam("uuid") String uuid) {
        try {
            HorarioFuncionamento horarioFuncionamento = ControleHorariosFuncionamento.getInstance().getHorarioFuncionamentoByUUID(UUID.fromString(uuid));
            if (!horarioFuncionamento.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (ControleHorariosFuncionamento.getInstance().excluirHorarioFuncionamento(horarioFuncionamento)) {
                return Response.status(Response.Status.CREATED).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/alterarEstadoHorarioFuncionamento")
    public Response alterarEstadoHorarioFuncionamento(@QueryParam("uuid") String uuid) {
        try {
            HorarioFuncionamento horarioFuncionamento = ControleHorariosFuncionamento.getInstance().getHorarioFuncionamentoByUUID(UUID.fromString(uuid));
            if (!horarioFuncionamento.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            horarioFuncionamento.setAtivo(!horarioFuncionamento.isAtivo());
            if (ControleHorariosFuncionamento.getInstance().salvarHorarioFuncionamento(horarioFuncionamento)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleHorariosFuncionamento.getInstance().getHorarioFuncionamentoByUUID(horarioFuncionamento.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/clientes")
    public Response getClientes(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.OK).entity(builder.toJson(ControleClientes.getInstance().getClientes(token.getEstabelecimento()))).build();
            } else {
                return Response.status(Response.Status.OK).entity(builder.toJson(ControleClientes.getInstance().getClienteByUUID(UUID.fromString(uuid)))).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarCliente")
    public Response salvarCliente(@FormParam("cliente") String cli) {
        try {
            Cliente cliente = builder.fromJson(cli, Cliente.class);
            cliente.setEstabelecimento(token.getEstabelecimento());
            if (!cliente.getTelefoneMovel().isEmpty() && token.getSistemaDelivery().getDriver().getEstadoDriver() == EstadoDriver.LOGGED) {
                try {
                    Chat chat = token.getSistemaDelivery().getDriver().getFunctions().getChatByNumber("55" + Utilitarios.replaceAllNoDigit(cliente.getTelefoneMovel()));
                    if (chat != null) {
                        cliente.setChatId(chat.getId());
                    }
                } catch (Exception e) {
                    throw e;
                }
            }
            cliente.setCadastroRealizado(true);
            if (ControleClientes.getInstance().salvarCliente(cliente)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleClientes.getInstance().getClienteByUUID(cliente.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarRecarga")
    public Response salvarRecarga(@FormParam("recarga") String recarga) {
        try {
            RecargaCliente recargaCliente = builder.fromJson(recarga, RecargaCliente.class);
            recargaCliente.setCliente(ControleClientes.getInstance().getClienteByUUID(recargaCliente.getUuid_cliente()));
            recargaCliente.setEstabelecimento(token.getEstabelecimento());
            if (recargaCliente.getValor() < 0) {
                recargaCliente.setValor(recargaCliente.getValor() * -1);
                recargaCliente.setTipoRecarga(TipoRecarga.SAQUE);
            } else {
                recargaCliente.setTipoRecarga(TipoRecarga.DEPOSITO);
            }
            if (ControleRecargas.getInstance().salvarRecarga(recargaCliente)) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleRecargas.getInstance().getRecargaByUUID(recargaCliente.getUuid()))).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/recargasCliente")
    public Response getRecargasClientes(@QueryParam("uuid") String uuid) {
        try {
            List<RecargaCliente> recargaClientes = ControleClientes.getInstance().getClienteByUUID(UUID.fromString(uuid)).getRegargas();
            JsonObject object = new JsonObject();
            object.addProperty("saldo", ControleClientes.getInstance().getClienteByUUID(UUID.fromString(uuid)).getCreditosDisponiveis());
            JsonElement element = builder.toJsonTree(ControleClientes.getInstance().getClienteByUUID(UUID.fromString(uuid)).getRegargas(), new TypeToken<List<RecargaCliente>>() {
            }.getType());
            object.add("recargas", element);
            return Response.status(Response.Status.OK).entity(builder.toJson(object)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }


    @GET
    @Path("/alterarEstadoPedidos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response alterarEstadoPedidos() {
        boolean flag;
        try {
            if (token.getEstabelecimento().isOpenPedidos()) {
                flag = token.getSistemaDelivery().fecharPedidos();
            } else {
                flag = token.getSistemaDelivery().abrirPedidos();
            }
            if (flag) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(token.getEstabelecimento())).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/alterarEstadoChatBot")
    @Produces(MediaType.APPLICATION_JSON)
    public Response alterarEstadoChatBot() {
        token.getEstabelecimento().setOpenChatBot(!token.getEstabelecimento().isOpenChatBot());
        try {
            if (ControleEstabelecimentos.getInstance().salvarEstabelecimento(token.getEstabelecimento())) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(token.getEstabelecimento())).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/pedido")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedido(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Pedido pedido = ControlePedidos.getInstance().getPedidoByUUID(UUID.fromString(uuid));
            if (pedido == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!pedido.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            JsonElement element = builder.toJsonTree(pedido);
            JsonArray arrayItensPedidos = element.getAsJsonObject().get("produtos").getAsJsonArray();
            for (int y = 0; y < arrayItensPedidos.size(); y++) {
                JsonObject object = arrayItensPedidos.get(y).getAsJsonObject().get("produto").getAsJsonObject();
                object.remove("foto");
            }
            return Response.status(Response.Status.OK).entity(builder.toJson(element)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/pedidosClientes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedidosCliente(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Cliente cliente = ControleClientes.getInstance().getClienteByUUID(UUID.fromString(uuid));
            if (cliente == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            List<Pedido> pedidos = ControlePedidos.getInstance().getPedidosCliente(cliente);
            JsonElement element = builder.toJsonTree(pedidos);
            JsonArray array = element.getAsJsonArray();
            for (int x = 0; x < array.size(); x++) {
                JsonArray arrayItensPedidos = array.get(x).getAsJsonObject().get("produtos").getAsJsonArray();
                for (int y = 0; y < arrayItensPedidos.size(); y++) {
                    JsonObject object = arrayItensPedidos.get(y).getAsJsonObject().get("produto").getAsJsonObject();
                    object.remove("foto");
                }
            }
            return Response.status(Response.Status.OK).entity(builder.toJson(element)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/pedidosEstabelecimento")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedidosEstabelecimento() {
        try {
            List<Pedido> pedidos = ControlePedidos.getInstance().getPedidos(token.getEstabelecimento());
            JsonElement element = builder.toJsonTree(pedidos);
            JsonArray array = element.getAsJsonArray();
            for (int x = 0; x < array.size(); x++) {
                JsonArray arrayItensPedidos = array.get(x).getAsJsonObject().get("produtos").getAsJsonArray();
                for (int y = 0; y < arrayItensPedidos.size(); y++) {
                    JsonObject object = arrayItensPedidos.get(y).getAsJsonObject().get("produto").getAsJsonObject();
                    object.remove("foto");
                }
            }
            return Response.status(Response.Status.OK).entity(builder.toJson(element)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/pedidosAtivos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedidosAtivos() {
        try {
            List<Pedido> pedidos = ControlePedidos.getInstance().getPedidosAtivos(token.getEstabelecimento());
            JsonElement element = builder.toJsonTree(pedidos);
            JsonArray array = element.getAsJsonArray();
            for (int x = 0; x < array.size(); x++) {
                JsonArray arrayItensPedidos = array.get(x).getAsJsonObject().get("produtos").getAsJsonArray();
                for (int y = 0; y < arrayItensPedidos.size(); y++) {
                    JsonObject object = arrayItensPedidos.get(y).getAsJsonObject().get("produto").getAsJsonObject();
                    object.remove("foto");
                }
            }
            return Response.status(Response.Status.OK).entity(builder.toJson(element)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/pedidosImprimir")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedidosImprimir() {
        try {
            List<Pedido> pedidos = ControlePedidos.getInstance().getPedidosNaoImpressos(token.getEstabelecimento());
            JsonElement element = builder.toJsonTree(pedidos);
            JsonArray array = element.getAsJsonArray();
            for (int x = 0; x < array.size(); x++) {
                JsonArray arrayItensPedidos = array.get(x).getAsJsonObject().get("produtos").getAsJsonArray();
                for (int y = 0; y < arrayItensPedidos.size(); y++) {
                    JsonObject object = arrayItensPedidos.get(y).getAsJsonObject().get("produto").getAsJsonObject();
                    object.remove("foto");
                }
            }
            return Response.status(Response.Status.OK).entity(builder.toJson(element)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/pedidoImpresso")
    @Produces(MediaType.TEXT_PLAIN)
    public Response pedidoImpresso(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Pedido pedido = ControlePedidos.getInstance().getPedidoByUUID(UUID.fromString(uuid));
            if (pedido == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!pedido.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            pedido.setImpresso(true);
            if (ControlePedidos.getInstance().salvarPedido(pedido)) {
                return Response.status(Response.Status.CREATED).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/pedidoSaiuEntrega")
    @Produces(MediaType.TEXT_PLAIN)
    public Response pedidoSaiuEntrega(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Pedido pedido = ControlePedidos.getInstance().getPedidoByUUID(UUID.fromString(uuid));
            if (pedido == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!pedido.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            pedido.setEstadoPedido(EstadoPedido.SaiuEntrega);
            if (ControlePedidos.getInstance().salvarPedido(pedido)) {
                return Response.status(Response.Status.CREATED).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/pedidoConcluido")
    @Produces(MediaType.TEXT_PLAIN)
    public Response pedidoConcluido(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Pedido pedido = ControlePedidos.getInstance().getPedidoByUUID(UUID.fromString(uuid));
            if (pedido == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!pedido.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            pedido.setEstadoPedido(EstadoPedido.Concluido);
            if (ControlePedidos.getInstance().salvarPedido(pedido)) {
                return Response.status(Response.Status.CREATED).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/pedidoCancelado")
    @Produces(MediaType.TEXT_PLAIN)
    public Response pedidoCancelado(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            Pedido pedido = ControlePedidos.getInstance().getPedidoByUUID(UUID.fromString(uuid));
            if (pedido == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!pedido.getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            pedido.setEstadoPedido(EstadoPedido.Cancelado);
            if (ControlePedidos.getInstance().salvarPedido(pedido)) {
                return Response.status(Response.Status.CREATED).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/removerItemPedido")
    @Produces(MediaType.TEXT_PLAIN)
    public Response removerItemPedido(@QueryParam("uuid") String uuid) {
        try {
            if (uuid == null || uuid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            ItemPedido itemPedido = ControleItensPedidos.getInstance().getItemByUUID(UUID.fromString(uuid));
            if (itemPedido == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (!itemPedido.getPedido().getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (itemPedido.getPedido().getEstadoPedido() == EstadoPedido.Concluido || itemPedido.getPedido().getEstadoPedido() == EstadoPedido.Cancelado) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (ControleItensPedidos.getInstance().excluirPedido(itemPedido) && ControlePedidos.getInstance().salvarPedido(itemPedido.getPedido())) {
                return Response.status(Response.Status.CREATED).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/criarPedidoTeste")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pedidoTeste() {
        try {
            Pedido p = new Pedido(ControleClientes.getInstance().getClienteChatId("554491050665@c.us", token.getEstabelecimento()), token.getEstabelecimento());

            List<Produto> produtosDisponiveis = ControleProdutos.getInstance().getProdutosEstabelecimento(token.getEstabelecimento());
            if (produtosDisponiveis.size() > 0) {
                Collections.shuffle(produtosDisponiveis);
                List<TipoEntrega> tipoEntregas = new ArrayList<>(token.getEstabelecimento().getTiposEntregas());
                Collections.shuffle(tipoEntregas);
                TipoEntrega tipoEntrega = tipoEntregas.get(0);
                p.setTipoEntrega(tipoEntrega);
                if (tipoEntrega.isSolicitarEndereco()) {
                    p.setEntrega(true);
                    Endereco endereco = new Endereco();
                    endereco.setLogradouro("Rua dos Alfineiros");
                    endereco.setNumero("4");
                    endereco.setBairro("Quarto embaixo da escada");
                    p.setEndereco(endereco);
                } else {
                    p.setEntrega(false);
                }
                if (new Random().nextInt() % 2 == 0) {
                    p.setHoraAgendamento(Time.valueOf(LocalTime.now()));
                }
                p.setCartao(new Random().nextInt() % 2 == 0);
                for (Produto produto : produtosDisponiveis) {
                    ItemPedido itemPedido = new ItemPedido();
                    itemPedido.setProduto(produto);
                    List<GrupoAdicional> grupoAdicionals = produto.getAllGruposAdicionais();
                    if (grupoAdicionals.size() > 0) {
                        for (GrupoAdicional grupoAdicional : grupoAdicionals) {
                            List<AdicionalProduto> adicionalProdutos = new ArrayList<>(grupoAdicional.getAdicionais());
                            if (adicionalProdutos.size() > 0) {
                                Collections.shuffle(adicionalProdutos);
                                for (AdicionalProduto adicionalProduto : adicionalProdutos) {
                                    itemPedido.addAdicional(adicionalProduto);
                                }
                            }
                        }
                    }
                    itemPedido.setQtd(new Random().nextInt(10) + 1);
                    if (new Random().nextInt() % 2 == 0) {
                        itemPedido.setComentario("Sem salada");
                    }
                    p.addItemPedido(itemPedido);
                }
                if (!p.isCartao()) {
                    p.calcularValor();
                    p.setTroco(p.getTotal() + new Random().nextInt(((int) p.getTotal())));
                }
                if (ControlePedidos.getInstance().salvarPedido(p)) {
                    return Response.status(Response.Status.CREATED).entity(builder.toJson(ControlePedidos.getInstance().getPedidoByUUID(p.getUuid()))).build();
                } else {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                }
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/entregasPorHorario")
    @Produces(MediaType.APPLICATION_JSON)
    public Response entregasPorHorario(@QueryParam("dataInicio") String data1) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date dataInicio = dateFormat.parse(data1);
            HashMap<Integer, Integer> entregas = ControlePedidos.getInstance().getEntregasPorHorario(token.getEstabelecimento(), dataInicio);
            JsonObject object = new JsonObject();
            object.add("entregas", builder.toJsonTree(entregas));
            object.addProperty("data", data1);
            return Response.status(Response.Status.OK).entity(builder.toJson(object)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/entregasPorDiaDaSemana")
    @Produces(MediaType.APPLICATION_JSON)
    public Response entregasPorDiaDaSemana(@QueryParam("dataInicio") String data1, @QueryParam("dataFim") String data2) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date dataInicio = dateFormat.parse(data1);
            Date dataFim = dateFormat.parse(data2);
            HashMap<String, Integer> entregas = ControlePedidos.getInstance().getEntregasPorDiaSemana(token.getEstabelecimento(), dataInicio, dataFim);
            JsonObject object = new JsonObject();
            object.add("entregas", builder.toJsonTree(entregas));
            object.addProperty("datas", data1 + " - " + data2);
            return Response.status(Response.Status.OK).entity(builder.toJson(object)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/receitaPorPeriodo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response receitaPorPeriodo(@QueryParam("dataInicio") String data1, @QueryParam("dataFim") String data2) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date dataInicio = dateFormat.parse(data1);
            Date dataFim = dateFormat.parse(data2);
            HashMap<String, HashMap<String, Double>> lista = ControlePedidos.getInstance().getReceitaPeriodo(token.getEstabelecimento(), dataInicio, dataFim);
            JsonObject object = new JsonObject();
            JsonArray array = new JsonArray();
            JsonArray array2 = new JsonArray();
            JsonArray array3 = new JsonArray();
            JsonArray array4 = new JsonArray();

            for (Map.Entry<String, Double> entry : lista.get("entrega").entrySet()) {
                array.add(entry.getValue());
                array4.add(entry.getKey());
            }
            for (Map.Entry<String, Double> entry : lista.get("retirada").entrySet()) {
                array2.add(entry.getValue());
            }
            for (Map.Entry<String, Double> entry : lista.get("total").entrySet()) {
                array3.add(entry.getValue());
            }

            JsonArray arrayData = new JsonArray();
            JsonObject entregas = new JsonObject();
            entregas.addProperty("name", "Entrega");
            entregas.add("data", array);

            JsonObject retirada = new JsonObject();
            retirada.addProperty("name", "Retirada");
            retirada.add("data", array2);

            JsonObject total = new JsonObject();
            total.addProperty("name", "Total");
            total.add("data", array3);

            arrayData.add(entregas);
            arrayData.add(retirada);
            arrayData.add(total);

            object.add("series", arrayData);
            object.add("meses", array4);
            object.addProperty("datas", data1 + " - " + data2);
            return Response.status(Response.Status.OK).entity(builder.toJson(object)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/vendasPorProduto")
    @Produces(MediaType.APPLICATION_JSON)
    public Response vendasPorProduto(@QueryParam("dataInicio") String data1, @QueryParam("dataFim") String data2) {
        try {
            HashMap<String, Integer> top5VendidosMes = new HashMap<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date dataInicio = dateFormat.parse(data1);
            Date dataFim = dateFormat.parse(data2);
            List<Pedido> pedidosMes = ControlePedidos.getInstance().getPedidosBetween(token.getEstabelecimento(), dataInicio, dataFim);
            for (Pedido pedido : pedidosMes) {
                synchronized (pedido.getProdutos()) {
                    for (ItemPedido itemPedido : pedido.getProdutos()) {
                        if (itemPedido.isRemovido()) {
                            continue;
                        }
                        String stringAtual = itemPedido.getProduto().getNomeWithCategories() + "";
                        if (!itemPedido.getAdicionais().isEmpty()) {
                            stringAtual += " - ";
                            for (Map.Entry<GrupoAdicional, List<AdicionalProduto>> entry : itemPedido.getAdicionaisGroupByGrupo().entrySet()) {
                                if (entry.getValue().isEmpty()) {
                                    continue;
                                }
                                stringAtual += entry.getKey().getNomeGrupo() + ": ";
                                for (AdicionalProduto adicionalProduto : entry.getValue()) {
                                    stringAtual += adicionalProduto.getNome() + ", ";
                                }
                                stringAtual = stringAtual.substring(0, stringAtual.lastIndexOf(",")).trim() + ". ";
                            }
                        }
                        stringAtual = stringAtual.trim();
                        if (top5VendidosMes.containsKey(stringAtual)) {
                            top5VendidosMes.put(stringAtual, top5VendidosMes.get(stringAtual) + itemPedido.getQtd());
                        } else {
                            top5VendidosMes.put(stringAtual, itemPedido.getQtd());
                        }
                    }
                }
            }
            LinkedHashMap<String, Integer> sortedMap = top5VendidosMes.entrySet().stream().sorted(Map.Entry.comparingByValue((t, t1) -> Integer.compare(t1, t))).limit(5).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            JsonObject object = new JsonObject();
            JsonArray array = new JsonArray();
            for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
                JsonObject object1 = new JsonObject();
                object1.addProperty("nome", entry.getKey());
                object1.addProperty("qtd", entry.getValue());
                array.add(object1);
            }
            object.add("top5Vendidos", builder.toJsonTree(array));
            object.addProperty("data", data1 + " - " + data2);
            return Response.status(Response.Status.OK).entity(builder.toJson(object)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/dadosDashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dadosDashboard() {
        try {
            HashMap<String, Integer> dadosDeliveryDia = ControlePedidos.getInstance().getDadosDeliveryHoje(token.getEstabelecimento());
            HashMap<String, Integer> top5VendidosMes = new HashMap<>();
            List<Pedido> pedidosMes = ControlePedidos.getInstance().getPedidosDoMes(token.getEstabelecimento());
            for (Pedido pedido : pedidosMes) {
                synchronized (pedido.getProdutos()) {
                    for (ItemPedido itemPedido : pedido.getProdutos()) {
                        if (itemPedido.isRemovido()) {
                            continue;
                        }
                        String stringAtual = itemPedido.getProduto().getNomeWithCategories() + "";
                        if (!itemPedido.getAdicionais().isEmpty()) {
                            stringAtual += " - ";
                            for (Map.Entry<GrupoAdicional, List<AdicionalProduto>> entry : itemPedido.getAdicionaisGroupByGrupo().entrySet()) {
                                if (entry.getValue().isEmpty()) {
                                    continue;
                                }
                                stringAtual += entry.getKey().getNomeGrupo() + ": ";
                                for (AdicionalProduto adicionalProduto : entry.getValue()) {
                                    stringAtual += adicionalProduto.getNome() + ", ";
                                }
                                stringAtual = stringAtual.substring(0, stringAtual.lastIndexOf(",")).trim() + ". ";
                            }
                        }
                        stringAtual = stringAtual.trim();
                        if (top5VendidosMes.containsKey(stringAtual)) {
                            top5VendidosMes.put(stringAtual, top5VendidosMes.get(stringAtual) + itemPedido.getQtd());
                        } else {
                            top5VendidosMes.put(stringAtual, itemPedido.getQtd());
                        }
                    }
                }
            }
            LinkedHashMap<String, Integer> sortedMap = top5VendidosMes.entrySet().stream().sorted(Map.Entry.comparingByValue((t, t1) -> Integer.compare(t1, t))).limit(5).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            JsonObject object = new JsonObject();
            object.add("dadosDeliveryDia", builder.toJsonTree(dadosDeliveryDia));
            JsonArray array = new JsonArray();
            for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
                JsonObject object1 = new JsonObject();
                object1.addProperty("nome", entry.getKey());
                object1.addProperty("qtd", entry.getValue());
                array.add(object1);
            }
            object.add("top5Vendidos", builder.toJsonTree(array));
            return Response.status(Response.Status.OK).entity(builder.toJson(object)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @POST
    @Path("/formatarMsg")
    @Produces(MediaType.TEXT_PLAIN)
    public Response formatarMsg(@FormParam("msg") String msg, @QueryParam("uuid-cliente") String uuid) {
        try {
            Cliente cliente = null;
            if (uuid != null && !uuid.isEmpty()) {
                cliente = ControleClientes.getInstance().getClienteByUUID(UUID.fromString(uuid));
            }
            int horaAtual = LocalTime.now().getHour();
            String saudacao = "";
            if (horaAtual >= 2 && horaAtual < 12) {
                saudacao = "Bom Dia";
            } else if (horaAtual >= 12 && horaAtual < 18) {
                saudacao = "Boa Tarde";
            } else {
                saudacao = "Boa Noite";
            }
            String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy"));
            String amanha = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy"));
            msg = msg.replaceAll("\\{estabelecimento}", token.getEstabelecimento().getNomeEstabelecimento()).replaceAll("\\{saudacao}", saudacao).replaceAll("\\{hoje}", hoje).replaceAll("\\{amanha}", amanha);
            if (cliente != null) {
                msg = msg.replaceAll("\\{cliente}", cliente.getNome());
            }
            return Response.status(Response.Status.OK).entity(msg).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/listasTransmissao")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getListasTransmissao() {
        try {
            WebWhatsDriver driver = token.getSistemaDelivery().getDriver();
            if (driver.getEstadoDriver() != EstadoDriver.LOGGED) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            JsonArray array = new JsonArray();
            List<BroadcastChat> listas = driver.getFunctions().getAllBroadcastChats();
            for (BroadcastChat chat : listas) {
                JsonObject ob = new JsonObject();
                ob.addProperty("nome", chat.getFormattedTitle());
                ob.addProperty("id", chat.getId());
                array.add(ob);
            }
            return Response.status(Response.Status.OK).entity(builder.toJson(array)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @POST
    @Path("/enviarMsg")
    @Produces(MediaType.TEXT_PLAIN)
    public Response enviarMsg(@FormParam("msg") String msg, @QueryParam("uuid-cliente") String uuid, @QueryParam("chatId") String chatid) {
        try {
            Cliente cliente = null;
            if (uuid != null && !uuid.isEmpty()) {
                cliente = ControleClientes.getInstance().getClienteByUUID(UUID.fromString(uuid));
            } else if (chatid == null || chatid.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (cliente == null && (chatid == null || chatid.isEmpty())) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            int horaAtual = LocalTime.now().getHour();
            String saudacao = "";
            if (horaAtual >= 2 && horaAtual < 12) {
                saudacao = "Bom Dia";
            } else if (horaAtual >= 12 && horaAtual < 18) {
                saudacao = "Boa Tarde";
            } else {
                saudacao = "Boa Noite";
            }
            String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy"));
            String amanha = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy"));
            msg = msg.replaceAll("\\{estabelecimento}", token.getEstabelecimento().getNomeEstabelecimento()).replaceAll("\\{saudacao}", saudacao).replaceAll("\\{hoje}", hoje).replaceAll("\\{amanha}", amanha);
            if (cliente != null) {
                msg = msg.replaceAll("\\{cliente}", cliente.getNome());
            }
            WebWhatsDriver driver = token.getSistemaDelivery().getDriver();
            if (driver.getEstadoDriver() != EstadoDriver.LOGGED) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            if (cliente != null) {
                driver.getFunctions().getChatById(cliente.getChatId()).sendMessage(msg);
            } else {
                driver.getFunctions().getChatById(chatid).sendMessage(msg);
            }
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/chats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChats() {
        try {
            if (token.getSistemaDelivery().getDriver().getEstadoDriver() != EstadoDriver.LOGGED) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            JsonArray array = new JsonArray();
            List<Chat> chats = token.getSistemaDelivery().getDriver().getFunctions().getAllChats(100);
            for (Chat c : chats) {
                JsonObject object = (JsonObject) builder.toJsonTree(parser.parse(c.toJson()));
                object.addProperty("noEarlierMsgs", c.noEarlierMsgs());
                object.add("contact", builder.toJsonTree(parser.parse(c.getContact().toJson())));
                Cliente cliente = ControleClientes.getInstance().getClienteChatId(c.getId(), token.getEstabelecimento());
                if (cliente != null) {
                    object.add("cliente", builder.toJsonTree(cliente));
                }
                array.add(object);
            }
            return Response.status(Response.Status.OK).entity(builder.toJson(array)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/mediaMessage")
    public Response mediaMessage(@QueryParam("msgId") String msgid) {
        if (token.getSistemaDelivery().getDriver().getEstadoDriver() != EstadoDriver.LOGGED) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        Message message = token.getSistemaDelivery().getDriver().getFunctions().getMessageById(msgid);
        if (message != null && message instanceof MediaMessage) {
            File file = ((MediaMessage) message).downloadMedia();
            try {
                byte[] data = Files.readAllBytes(file.toPath());
                String fileName = ((MediaMessage) message).getFileName();
                if (fileName.isEmpty()) {
                    fileName = file.getName();
                }
                return Response.status(Response.Status.OK).header("Content-disposition", "attachment;filename=\"" + fileName + "\"").type(((MediaMessage) message).getMime()).entity(data).build();
            } catch (Exception e) {
                Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
            }
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/pictureChat")
    @Produces("image/png")
    public Response pictureChat(@QueryParam("chatId") String chatid) {
        try {
            if (token.getSistemaDelivery().getDriver().getEstadoDriver() != EstadoDriver.LOGGED) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            Chat chat = token.getSistemaDelivery().getDriver().getFunctions().getChatById(chatid);
            if (chat != null) {
                String img = chat.getContact().getThumb();
                if (img.isEmpty()) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
                return Response.status(Response.Status.OK).entity(Base64.getDecoder().decode(img.split(",")[1])).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/sendSeenChat")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendSeenChat(@QueryParam("chatId") String chatid) {
        try {
            if (token.getSistemaDelivery().getDriver().getEstadoDriver() != EstadoDriver.LOGGED) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            Chat chat = token.getSistemaDelivery().getDriver().getFunctions().getChatById(chatid);
            if (chat == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            chat.sendSeen(false);
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/loadEarly")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadEarly(@QueryParam("chatId") String chatid) {
        try {
            if (token.getSistemaDelivery().getDriver().getEstadoDriver() != EstadoDriver.LOGGED) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            Chat chat = token.getSistemaDelivery().getDriver().getFunctions().getChatById(chatid);
            if (chat == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            chat.loadEarlierMsgs(() -> {
                if (token.getSistemaDelivery().getBroadcasterWhats() != null) {
                    JsonObject object = (JsonObject) builder.toJsonTree(parser.parse(chat.toJson()));
                    object.addProperty("noEarlierMsgs", chat.noEarlierMsgs());
                    object.add("contact", builder.toJsonTree(parser.parse(chat.getContact().toJson())));
                    token.getSistemaDelivery().getBroadcasterWhats().broadcast(token.getSistemaDelivery().getSseWhats().newEvent("chat-update", builder.toJson(object)));
                }
            });
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/msgsChat")
    @Produces(MediaType.APPLICATION_JSON)
    public Response msgsChat(@QueryParam("chatId") String chatid) {
        try {
            if (token.getSistemaDelivery().getDriver().getEstadoDriver() != EstadoDriver.LOGGED) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            Chat chat = token.getSistemaDelivery().getDriver().getFunctions().getChatById(chatid);
            if (chat == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            List<Message> msgs = token.getSistemaDelivery().getDriver().getFunctions().getAllMessagesInChat(chat, true, true);
            JsonArray array = new JsonArray();
            for (Message msg : msgs) {
                if (msg instanceof MediaMessage) {
                    JsonObject object = (JsonObject) builder.toJsonTree(parser.parse(msg.toJson()));
                    MediaMessage mediaMessage = (MediaMessage) msg;
                    File file = mediaMessage.downloadMedia();
                    String contentType = Files.probeContentType(file.toPath());

                    // read data as byte[]
                    byte[] data = Files.readAllBytes(file.toPath());

                    // convert byte[] to base64(java7)
                    //String base64str = DatatypeConverter.printBase64Binary(data);
                    // convert byte[] to base64(java8)
                    String base64str = Base64.getEncoder().encodeToString(data);

                    // cretate "data URI"
                    StringBuilder sb = new StringBuilder();
                    sb.append("data:");
                    sb.append(contentType);
                    sb.append(";base64,");
                    sb.append(base64str);
                    object.addProperty("mediaBase64", sb.toString());
                    array.add(object);
                } else {
                    array.add(builder.toJsonTree(parser.parse(msg.getJsObject().toJSONString())));
                }
            }
            return Response.status(Response.Status.OK).entity(builder.toJson(array)).build();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/finalizar")
    @Produces(MediaType.TEXT_PLAIN)
    public Response finalizar() {
        try {
            token.getEstabelecimento().setOpenChatBot(false);
            if (ControleEstabelecimentos.getInstance().salvarEstabelecimento(token.getEstabelecimento())) {
                return Response.status(Response.Status.CREATED).entity(builder.toJson(token.getEstabelecimento())).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @GET
    @Path("/logout")
    @Produces(MediaType.TEXT_PLAIN)
    public Response logout() {
        try {
            token.getSistemaDelivery().logout();
        } catch (Exception e) {
            Logger.getLogger(token.getEstabelecimento().getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ExceptionUtils.getStackTrace(e)).build();
        }
        return Response.status(Response.Status.OK).build();
    }
}
