package restFul;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import driver.WebWhatsDriver;
import modelo.Chat;
import modelo.EstadoDriver;
import restFul.controle.ControleSessions;
import restFul.controle.ControleTokens;
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
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Time;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Path("/api")
public class API {

    private Gson builder;
    private Token token;
    private List<Pedido> pedidosSendoCriados;

    public API(@Context HttpServletRequest session, @Context SecurityContext securityContext) {
        this.token = (Token) securityContext.getUserPrincipal();
        builder = Utilitarios.getDefaultGsonBuilder(null).create();
        if (session.getSession(true).getAttribute("pedidosSendoCriados") == null) {
            session.getSession().setAttribute("pedidosSendoCriados", Collections.synchronizedList(new ArrayList<>()));
        }
        pedidosSendoCriados = ((List<Pedido>) session.getSession().getAttribute("pedidosSendoCriados"));
    }

    @GET
    @Path("/eventos")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void eventoNovoPedido(@Context SseEventSink sink, @Context Sse sse) {
        try {
            SistemaDelivery sistemaDelivery = ControleSessions.getInstance().getSessionForEstabelecimento(token.getEstabelecimento());
            if (sistemaDelivery.getBroadcaster() != null) {
                sistemaDelivery.getBroadcaster().register(sink);
            } else {
                sistemaDelivery.setSse(sse);
                sistemaDelivery.setBroadcaster(sse.newBroadcaster());
                sistemaDelivery.getBroadcaster().register(sink);
            }
            sink.send(sse.newEvent("ok"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @GET
    @Path("/iniciarNovoPedido")
    @Produces(MediaType.APPLICATION_JSON)
    public Response iniciarNovoPedido() {
        Pedido pedido = new Pedido();
        pedido.setEstabelecimento(token.getEstabelecimento());
        pedido.setUuid(UUID.randomUUID());
        synchronized (pedidosSendoCriados) {
            pedidosSendoCriados.add(pedido);
        }
        return Response.status(Response.Status.CREATED).entity(builder.toJson(pedido)).build();
    }

    @GET
    @Path("/infoPedidoSendoCriado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response infoPedidoSendoCriado(@QueryParam("uuid") String uuid) {
        synchronized (pedidosSendoCriados) {
            for (Pedido pedido : pedidosSendoCriados) {
                if (pedido.getUuid().equals(UUID.fromString(uuid))) {
                    return Response.status(Response.Status.CREATED).entity(builder.toJson(pedido)).build();
                }
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInfo() {
        return Response.status(Response.Status.OK).entity(builder.toJson(token)).build();
    }


    @GET
    @Path("/estadoWhats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response estadoWhats() {
        JsonObject object = new JsonObject();
        try {
            WebWhatsDriver driver = ControleSessions.getInstance().getSessionForEstabelecimento(token.getEstabelecimento()).getDriver();
            object.addProperty("status", driver.getEstadoDriver().toString());
            if (driver.getEstadoDriver() == EstadoDriver.WAITING_QR_CODE_SCAN) {
                object.addProperty("qrCode", driver.getQrCodePlain());
            }
            return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(builder.toJson(object)).build();
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/alterarEstabelecimento")
    @Produces(MediaType.APPLICATION_JSON)
    public Response alterarEstabelecimento(@FormParam("estabelecimento") String estabelecimento) {
        Estabelecimento novosValoresEstabelecimento = builder.fromJson(estabelecimento, Estabelecimento.class);
        token.getEstabelecimento().setAbrirFecharPedidosAutomaticamente(novosValoresEstabelecimento.isAbrirFecharPedidosAutomaticamente());
        token.getEstabelecimento().setAgendamentoDePedidos(novosValoresEstabelecimento.isAgendamentoDePedidos());
        token.getEstabelecimento().setHoraAutomaticaAbrirPedidos(novosValoresEstabelecimento.getHoraAutomaticaAbrirPedidos());
        token.getEstabelecimento().setHoraAutomaticaFecharPedidos(novosValoresEstabelecimento.getHoraAutomaticaFecharPedidos());
        token.getEstabelecimento().setHoraInicioReservas(novosValoresEstabelecimento.getHoraInicioReservas());
        token.getEstabelecimento().setNomeBot(novosValoresEstabelecimento.getNomeBot());
        token.getEstabelecimento().setNomeEstabelecimento(novosValoresEstabelecimento.getNomeEstabelecimento());
        token.getEstabelecimento().setNumeroAviso(novosValoresEstabelecimento.getNumeroAviso());
        token.getEstabelecimento().setReservas(novosValoresEstabelecimento.isReservas());
        token.getEstabelecimento().setReservasComPedidosFechados(novosValoresEstabelecimento.isReservasComPedidosFechados());
        token.getEstabelecimento().setTaxaEntregaFixa(novosValoresEstabelecimento.getTaxaEntregaFixa());
        token.getEstabelecimento().setTaxaEntregaKm(novosValoresEstabelecimento.getTaxaEntregaKm());
        token.getEstabelecimento().setWebHookNovoPedido(novosValoresEstabelecimento.getWebHookNovoPedido());
        token.getEstabelecimento().setWebHookNovaReserva(novosValoresEstabelecimento.getWebHookNovaReserva());
        token.getEstabelecimento().setLogo(novosValoresEstabelecimento.getLogo());
        token.getEstabelecimento().setValidadeSeloFidelidade(novosValoresEstabelecimento.getValidadeSeloFidelidade());
        token.getEstabelecimento().setValorSelo(novosValoresEstabelecimento.getValorSelo());
        token.getEstabelecimento().setMaximoSeloPorCompra(novosValoresEstabelecimento.getMaximoSeloPorCompra());
        if (ControleEstabelecimentos.getInstance().salvarEstabelecimento(token.getEstabelecimento())) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(token.getEstabelecimento())).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GET
    @Path("/categorias")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCategorias(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Path("/gruposAdicionaisCategoria")
    @Produces(MediaType.APPLICATION_JSON)
    public Response gruposAdicionaisCategoria(@QueryParam("uuid") String uuid) {
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
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarCategoria")
    public Response salvarCategoria(@FormParam("categoria") String cat) {
        Categoria categoria = builder.fromJson(cat, Categoria.class);
        categoria.setEstabelecimento(token.getEstabelecimento());
        categoria.setCategoriaPai(ControleCategorias.getInstance().getCategoriaByUUID(categoria.getUuid_categoria_pai()));
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
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/alterarVisibilidadeCategoria")
    public Response alterarVisibilidadeCategoria(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/alterarEntregaGratisCategoria")
    public Response alterarEntregaGratisCategoria(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirCategoria")
    public Response excluirCategoria(@QueryParam("uuid") String uuid) {
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
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarProduto")
    public Response salvarProduto(@FormParam("produto") String prod) {
        Produto produto = builder.fromJson(prod, Produto.class);
        produto.setCategoria(ControleCategorias.getInstance().getCategoriaByUUID(produto.getUuid_categoria()));
        if (!produto.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (ControleProdutos.getInstance().salvarProduto(produto)) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleProdutos.getInstance().getProdutoByUUID(produto.getUuid()))).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/produtos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProdutos(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Path("/produto")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProduto(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Path("/gruposAdicionaisProduto")
    @Produces(MediaType.APPLICATION_JSON)
    public Response gruposAdicionaisProduto(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/alterarVisibilidadeProduto")
    public Response alterarVisibilidadeProduto(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirProduto")
    public Response excluirProduto(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/rodizios")
    public Response getRodizios(@QueryParam("uuid") String uuid) {
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
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarRodizio")
    public Response salvarRodizio(@FormParam("rodizio") String rod) {
        Rodizio rodizio = builder.fromJson(rod, Rodizio.class);
        rodizio.setEstabelecimento(token.getEstabelecimento());
        if (ControleRodizios.getInstace().salvarRodizio(rodizio)) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleRodizios.getInstace().getRodizioByUUID(rodizio.getUuid()))).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirRodizio")
    public Response excluirRodizio(@QueryParam("uuid") String uuid) {
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
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarGrupoAdicional")
    public Response salvarGrupoAdicional(@FormParam("grupo") String grupo) {
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
    }

    @GET
    @Path("/grupoAdicionais")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGrupoAdicionais(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirGrupoAdicional")
    public Response excluirGrupoAdicional(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/exportarGrupoAdicional")
    public Response exportarGrupoAdicional(@QueryParam("uuid") String uuid) {
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
        } catch (Exception ex) {
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
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
        } catch (Exception ex) {
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarAdicional")
    public Response salvarAdicional(@FormParam("adicional") String adicional) {
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
    }

    @GET
    @Path("/adicional")
    @Produces(MediaType.APPLICATION_JSON)
    public Response adicional(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirAdicional")
    public Response excluirAdicional(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Path("/reservaImpressa")
    @Produces(MediaType.TEXT_PLAIN)
    public Response reservaImpressa(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Path("/reservasImprimir")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reservasImprimir() {
        return Response.status(Response.Status.OK).entity(builder.toJson(ControleReservas.getInstance().getReservasImprimir(token.getEstabelecimento()))).build();
    }

    @GET
    @Path("/reservas")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReservas(@QueryParam("uuid") String uuid) {
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
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarReserva")
    public Response salvarReserva(@FormParam("reserva") String res) {
        Reserva reserva = builder.fromJson(res, Reserva.class);
        reserva.setEstabelecimento(token.getEstabelecimento());
        reserva.setCliente(ControleClientes.getInstance().getClienteByUUID(reserva.getUuid_cliente()));
        if (ControleReservas.getInstance().salvarReserva(reserva)) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleReservas.getInstance().getReservaByUUID(reserva.getUuid()))).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/excluirReserva")
    public Response excluirReserva(@QueryParam("uuid") String uuid) {
        Reserva reserva = ControleReservas.getInstance().getReservaByUUID(UUID.fromString(uuid));
        if (!reserva.getEstabelecimento().equals(token.getEstabelecimento())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (ControleReservas.getInstance().excluirReserva(reserva)) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleReservas.getInstance().getReservaByUUID(reserva.getUuid()))).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/clientes")
    public Response getClientes(@QueryParam("uuid") String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return Response.status(Response.Status.OK).entity(builder.toJson(ControleClientes.getInstance().getClientes(token.getEstabelecimento()))).build();
        } else {
            return Response.status(Response.Status.OK).entity(builder.toJson(ControleClientes.getInstance().getClienteByUUID(UUID.fromString(uuid)))).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarCliente")
    public Response salvarCliente(@FormParam("cliente") String cli) {
        Cliente cliente = builder.fromJson(cli, Cliente.class);
        if (!cliente.getTelefoneMovel().isEmpty()) {
            try {
                Chat chat = ControleSessions.getInstance().getSessionForEstabelecimento(token.getEstabelecimento()).getDriver().getFunctions().getChatByNumber("55" + cliente.getTelefoneMovel());
                if (chat != null) {
                    cliente.setChatId(chat.getId());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        cliente.setCadastroRealizado(true);
        if (ControleClientes.getInstance().salvarCliente(cliente)) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleClientes.getInstance().getClienteByUUID(cliente.getUuid()))).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarRecarga")
    public Response salvarRecarga(@FormParam("recarga") String recarga) {
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
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/recargasCliente")
    public Response getRecargasClientes(@QueryParam("uuid") String uuid) {
        List<RecargaCliente> recargaClientes = ControleClientes.getInstance().getClienteByUUID(UUID.fromString(uuid)).getRegargas();
        JsonObject object = new JsonObject();
        object.addProperty("saldo", ControleClientes.getInstance().getClienteByUUID(UUID.fromString(uuid)).getCreditosDisponiveis());
        JsonElement element = builder.toJsonTree(ControleClientes.getInstance().getClienteByUUID(UUID.fromString(uuid)).getRegargas(), new TypeToken<List<RecargaCliente>>() {
        }.getType());
        object.add("recargas", element);
        return Response.status(Response.Status.OK).entity(builder.toJson(object)).build();
    }


    @GET
    @Path("/alterarEstadoPedidos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response alterarEstadoPedidos() {
        boolean flag = false;
        if (token.getEstabelecimento().isOpenPedidos()) {
            try {
                flag = ControleSessions.getInstance().getSessionForEstabelecimento(token.getEstabelecimento()).fecharPedidos();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                flag = ControleSessions.getInstance().getSessionForEstabelecimento(token.getEstabelecimento()).abrirPedidos();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (flag) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(token.getEstabelecimento())).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/alterarEstadoChatBot")
    @Produces(MediaType.APPLICATION_JSON)
    public Response alterarEstadoChatBot() {
        token.getEstabelecimento().setOpenChatBot(!token.getEstabelecimento().isOpenChatBot());
        if (ControleEstabelecimentos.getInstance().salvarEstabelecimento(token.getEstabelecimento())) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(token.getEstabelecimento())).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/pedido")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedido(@QueryParam("uuid") String uuid) {
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
        return Response.status(Response.Status.OK).entity(builder.toJson(pedido)).build();
    }

    @GET
    @Path("/pedidosClientes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedidosCliente(@QueryParam("uuid") String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Cliente cliente = ControleClientes.getInstance().getClienteByUUID(UUID.fromString(uuid));
        if (cliente == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.OK).entity(builder.toJson(ControlePedidos.getInstance().getPedidosCliente(cliente))).build();
    }

    @GET
    @Path("/pedidosEstabelecimento")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedidosEstabelecimento() {
        return Response.status(Response.Status.OK).entity(builder.toJson(ControlePedidos.getInstance().getPedidos(token.getEstabelecimento()))).build();
    }

    @GET
    @Path("/pedidosAtivos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedidosAtivos() {
        return Response.status(Response.Status.OK).entity(builder.toJson(ControlePedidos.getInstance().getPedidosAtivos(token.getEstabelecimento()))).build();
    }

    @GET
    @Path("/pedidosImprimir")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getpedidosImprimir() {
        return Response.status(Response.Status.OK).entity(builder.toJson(ControlePedidos.getInstance().getPedidosNaoImpressos(token.getEstabelecimento()))).build();
    }

    @GET
    @Path("/pedidoImpresso")
    @Produces(MediaType.TEXT_PLAIN)
    public Response pedidoImpresso(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Path("/pedidoSaiuEntrega")
    @Produces(MediaType.TEXT_PLAIN)
    public Response pedidoSaiuEntrega(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Path("/pedidoConcluido")
    @Produces(MediaType.TEXT_PLAIN)
    public Response pedidoConcluido(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Path("/pedidoCancelado")
    @Produces(MediaType.TEXT_PLAIN)
    public Response pedidoCancelado(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Path("/removerItemPedido")
    @Produces(MediaType.TEXT_PLAIN)
    public Response removerItemPedido(@QueryParam("uuid") String uuid) {
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
    }

    @GET
    @Path("/criarPedidoTeste")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pedidoTeste() {
        Pedido p = new Pedido(ControleClientes.getInstance().getClienteChatId("554491050665@c.us", token.getEstabelecimento()), token.getEstabelecimento());

        List<Produto> produtosDisponiveis = ControleProdutos.getInstance().getProdutosEstabelecimento(token.getEstabelecimento());
        if (produtosDisponiveis.size() > 0) {
            Collections.shuffle(produtosDisponiveis);
            p.setEntrega(new Random().nextInt() % 2 == 0);
            if (p.isEntrega()) {
                Endereco endereco = new Endereco();
                endereco.setLogradouro("Rua dos Alfineiros");
                endereco.setNumero("4");
                endereco.setBairro("Quarto embaixo da escada");
                p.setEndereco(endereco);
            }
            if (new Random().nextInt() % 2 == 0) {
                p.setHoraAgendamento(Time.valueOf(LocalTime.now()));
            }
            p.setCartao(new Random().nextInt() % 2 == 0);
            for (int x = 0; x < new Random().nextInt(30) + 1; x++) {
                for (Produto produto : produtosDisponiveis) {
                    ItemPedido itemPedido = new ItemPedido();
                    p.getProdutos().add(itemPedido);
                    itemPedido.setProduto(produto);
                    List<GrupoAdicional> grupoAdicionals = produto.getAllGruposAdicionais();
                    if (grupoAdicionals.size() > 0) {
                        for (GrupoAdicional grupoAdicional : grupoAdicionals) {
                            List<AdicionalProduto> adicionalProdutos = grupoAdicional.getAdicionais();
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
                }
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
    }

    @GET
    @Path("/dadosDashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dadosDashboard() {
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
    }

    @GET
    @Path("/finalizar")
    @Produces(MediaType.TEXT_PLAIN)
    public Response finalizar() {
        ControleSessions.getInstance().finalizarSessionForEstabelecimento(token.getEstabelecimento());
        ControleTokens.getInstance().removerToken(token);
        return Response.status(Response.Status.OK).build();
    }

    @GET
    @Path("/logout")
    @Produces(MediaType.TEXT_PLAIN)
    public Response logout() {
        try {
            ControleSessions.getInstance().getSessionForEstabelecimento(token.getEstabelecimento()).logout();
        } catch (IOException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Response.Status.OK).build();
    }
}
