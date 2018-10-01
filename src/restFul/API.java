package restFul;

import adapters.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import driver.WebWhatsDriver;
import modelo.Chat;
import modelo.EstadoDriver;
import restFul.controle.ControleSessions;
import restFul.modelo.Token;
import sistemaDelivery.controle.*;
import sistemaDelivery.modelo.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Path("/api")
public class API {

    private Gson builder;

    private Token token;

    public API(@Context SecurityContext securityContext) {
        this.token = (Token) securityContext.getUserPrincipal();
        builder = new GsonBuilder().disableHtmlEscaping().
                registerTypeAdapter(LocalTime.class, new LocalTimeAdapter()).
                registerTypeAdapter(LocalTime.class, new LocalTimeAdapterDeserialize()).
                registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).
                registerTypeAdapter(LocalDate.class, new LocalDateAdapterDeserialize()).
                registerTypeAdapter(Time.class, new TimeAdapter()).
                registerTypeAdapter(Time.class, new TimeAdapterDeserialize()).
                setDateFormat("dd/MM/yyyy").
                create();
    }

    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInfo() {
        return Response.status(Response.Status.OK).entity(builder.toJson(token)).build();
    }


    @GET
    @Path("/adicional")
    @Produces(MediaType.APPLICATION_JSON)
    public Response adicional(@QueryParam("uuid") String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            AdicionalProduto adicional = ControleAdicionais.getInstace().getAdicionalByUUID(UUID.fromString(uuid));
            if (adicional == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            } else if (adicional.getGrupoAdicional().getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.OK).entity(builder.toJson(adicional)).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }
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
        if (ControleEstabelecimentos.getInstace().salvarEstabelecimento(token.getEstabelecimento())) {
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
            List<Categoria> categorias = ControleCategorias.getInstace().getCategoriasEstabelecimento(token.getEstabelecimento());
            String json = builder.toJson(categorias);
            return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(json).build();
        } else {
            Categoria categoria = ControleCategorias.getInstace().getCategoriaByUUID(UUID.fromString(uuid));
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
            Categoria categoria = ControleCategorias.getInstace().getCategoriaByUUID(UUID.fromString(uuid));
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
        categoria.setCategoriaPai(ControleCategorias.getInstace().getCategoriaByUUID(categoria.getUuid_categoria_pai()));
        for (UUID uuidCat : categoria.getUuidsCategoriasNecessarias()) {
            Categoria catNecessaria = ControleCategorias.getInstace().getCategoriaByUUID(uuidCat);
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
        if (ControleCategorias.getInstace().salvarCategoria(categoria)) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleCategorias.getInstace().getCategoriaByUUID(categoria.getUuid()))).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/alterarVisibilidadeCategoria")
    public Response alterarVisibilidadeCategoria(@QueryParam("uuid") String uuid) {
        Categoria categoria = ControleCategorias.getInstace().getCategoriaByUUID(UUID.fromString(uuid));
        if (!categoria.getEstabelecimento().equals(token.getEstabelecimento())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        categoria.setVisivel(!categoria.isVisivel());
        if (ControleCategorias.getInstace().salvarCategoria(categoria)) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleCategorias.getInstace().getCategoriaByUUID(categoria.getUuid()))).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/alterarEntregaGratisCategoria")
    public Response alterarEntregaGratisCategoria(@QueryParam("uuid") String uuid) {
        Categoria categoria = ControleCategorias.getInstace().getCategoriaByUUID(UUID.fromString(uuid));
        if (!categoria.getEstabelecimento().equals(token.getEstabelecimento())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        categoria.setEntregaGratis(!categoria.isEntregaGratis());
        if (ControleCategorias.getInstace().salvarCategoria(categoria)) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleCategorias.getInstace().getCategoriaByUUID(categoria.getUuid()))).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirCategoria")
    public Response excluirCategoria(@QueryParam("uuid") String uuid) {
        Categoria categoria = ControleCategorias.getInstace().getCategoriaByUUID(UUID.fromString(uuid));
        if (!categoria.getEstabelecimento().equals(token.getEstabelecimento())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (ControleCategorias.getInstace().excluirCategoria(categoria)) {
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
        produto.setCategoria(ControleCategorias.getInstace().getCategoriaByUUID(produto.getUuid_categoria()));
        if (!produto.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (ControleProdutos.getInstace().salvarProduto(produto)) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleProdutos.getInstace().getProdutoByUUID(produto.getUuid()))).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/produtos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProdutos(@QueryParam("uuid") String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else {
            Categoria categoria = ControleCategorias.getInstace().getCategoriaByUUID(UUID.fromString(uuid));
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
            Produto produto = ControleProdutos.getInstace().getProdutoByUUID(UUID.fromString(uuid));
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
            Produto produto = ControleProdutos.getInstace().getProdutoByUUID(UUID.fromString(uuid));
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
        Produto produto = ControleProdutos.getInstace().getProdutoByUUID(UUID.fromString(uuid));
        if (!produto.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        produto.setVisivel(!produto.isVisivel());
        if (ControleProdutos.getInstace().salvarProduto(produto)) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleProdutos.getInstace().getProdutoByUUID(produto.getUuid()))).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirProduto")
    public Response excluirProduto(@QueryParam("uuid") String uuid) {
        Produto produto = ControleProdutos.getInstace().getProdutoByUUID(UUID.fromString(uuid));
        if (!produto.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (ControleProdutos.getInstace().excluirProduto(produto)) {
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
            grupoAdicional.setCategoria(ControleCategorias.getInstace().getCategoriaByUUID(grupoAdicional.getUuid_categoria()));
            if (!grupoAdicional.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        } else if (grupoAdicional.getUuid_produto() != null) {
            grupoAdicional.setProduto(ControleProdutos.getInstace().getProdutoByUUID(grupoAdicional.getUuid_produto()));
            if (!grupoAdicional.getProduto().getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("uuid categoria ou produto faltando").build();
        }
        if (ControleGruposAdicionais.getInstace().salvarGrupoAdicional(grupoAdicional)) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleGruposAdicionais.getInstace().getGrupoByUUID(grupoAdicional.getUuid()))).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/excluirGrupoAdicional")
    public Response excluirGrupoAdicional(@QueryParam("uuid") String uuid) {
        GrupoAdicional grupoAdicional = ControleGruposAdicionais.getInstace().getGrupoByUUID(UUID.fromString(uuid));
        if (grupoAdicional.getCategoria() != null) {
            if (!grupoAdicional.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        } else if (grupoAdicional.getProduto() != null) {
            if (!grupoAdicional.getProduto().getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }
        if (ControleGruposAdicionais.getInstace().excluirGrupo(grupoAdicional)) {
            return Response.status(Response.Status.CREATED).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/salvarAdicional")
    public Response salvarAdicional(@FormParam("adicional") String adicional) {
        AdicionalProduto adicionalProduto = builder.fromJson(adicional, AdicionalProduto.class);
        GrupoAdicional grupoAdicional = ControleGruposAdicionais.getInstace().getGrupoByUUID(adicionalProduto.getUuid_grupo_adicional());
        if (grupoAdicional.getCategoria() != null) {
            if (!grupoAdicional.getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        } else if (!grupoAdicional.getProduto().getCategoria().getEstabelecimento().equals(token.getEstabelecimento())) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        adicionalProduto.setGrupoAdicional(grupoAdicional);
        if (ControleAdicionais.getInstace().salvarAdicional(adicionalProduto)) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(ControleAdicionais.getInstace().getAdicionalByUUID(adicionalProduto.getUuid()))).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/clientes")
    public Response getClientes(@QueryParam("uuid") String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return Response.status(Response.Status.OK).entity(builder.toJson(ControleClientes.getInstace().getClientes())).build();
        } else {
            return Response.status(Response.Status.OK).entity(builder.toJson(ControleClientes.getInstace().getClienteByUUID(UUID.fromString(uuid)))).build();
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
        if (ControleClientes.getInstace().salvarCliente(cliente)) {
            return Response.status(Response.Status.OK).entity(builder.toJson(ControleClientes.getInstace().getClienteByUUID(cliente.getUuid()))).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GET
    @Path("/alterarEstadoPedidos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response alterarEstadoPedidos() {
        token.getEstabelecimento().setOpenPedidos(!token.getEstabelecimento().isOpenPedidos());
        if (ControleEstabelecimentos.getInstace().salvarEstabelecimento(token.getEstabelecimento())) {
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
        if (ControleEstabelecimentos.getInstace().salvarEstabelecimento(token.getEstabelecimento())) {
            return Response.status(Response.Status.CREATED).entity(builder.toJson(token.getEstabelecimento())).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/finalizar")
    @Produces(MediaType.TEXT_PLAIN)
    public Response logout() {
        ControleSessions.getInstance().finalizarSessionForEstabelecimento(token.getEstabelecimento());
        return Response.status(Response.Status.OK).build();
    }
}
