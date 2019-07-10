package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import restFul.controle.ControleSessions;
import sistemaDelivery.SistemaDelivery;
import sistemaDelivery.modelo.*;
import utils.PedidoHandlerRowProcessor;
import utils.Utilitarios;

import java.io.IOException;
import java.sql.*;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ControlePedidos {

    private static final Lock lockWhenLoading = new ReentrantLock();
    private static final Object syncronizeGetSession = new Object();
    private static ControlePedidos instance;
    private Map<UUID, Pedido> pedidos;

    private ControlePedidos() {
        this.pedidos = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControlePedidos getInstance() {
        synchronized (syncronizeGetSession) {
            if (instance == null) {
                instance = new ControlePedidos();
            }
            return instance;
        }
    }

    public Pedido getPedidoByUUID(UUID uuid) throws SQLException {
        if (pedidos.containsKey(uuid)) {
            return pedidos.get(uuid);
        }
        synchronized (pedidos) {
            try {
                QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
                ResultSetHandler<Pedido> h = new BeanHandler<Pedido>(Pedido.class, new PedidoHandlerRowProcessor());
                Pedido pedido = queryRunner.query("select * from \"Pedidos\" where uuid = ?", h, uuid);
                if (pedido == null) {
                    return null;
                }
                pedidos.putIfAbsent(uuid, pedido);
                pedido.setCliente(ControleClientes.getInstance().getClienteByUUID(pedido.getUuid_cliente()));
                pedido.setEstabelecimento(ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(pedido.getUuid_estabelecimento()));
                pedido.setProdutos(ControleItensPedidos.getInstance().getItensPedidos(pedido));
                pedido.setTipoEntrega(ControleTiposEntrega.getInstance().getTipoEntregaByUUID(pedido.getUuid_tipoEntrega()));
                return pedidos.get(uuid);
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    public boolean salvarPedido(Pedido pedido) throws IOException, SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            if (pedido.getUuid() == null) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("select next_cod_estabelecimento(?)")) {
                    preparedStatement.setObject(1, pedido.getEstabelecimento().getUuid());
                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.next()) {
                        pedido.setCod(resultSet.getLong(1));
                    } else {
                        return false;
                    }
                }
                try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO \"Pedidos\"(\n" +
                        "            uuid, uuid_cliente, uuid_estabelecimento, \"comentarioPedido\", \n" +
                        "            entrega, cartao, impresso, \"valorPago\", desconto, \"pgCreditos\", \n" +
                        "            \"subTotal\", total, \"horaAgendamento\", \"estadoPedido\", \n" +
                        "            troco, \"totalRemovido\", logradouro, bairro, referencia, numero,cod,\"taxaEntrega\",\"uuid_tipoEntrega\",\"dataPedido\")\n" +
                        "    VALUES (?, ?, ?, ?, \n" +
                        "            ?, ?, ?, ?, ?, ?, \n" +
                        "            ?, ?, ?, ?, ?, \n" +
                        "            ?, ?, ?, ?, ?, ?,?,?,?);\n")) {
                    pedido.setUuid(UUID.randomUUID());
                    pedido.calcularValor();
                    preparedStatement.setObject(1, pedido.getUuid());
                    preparedStatement.setObject(2, pedido.getCliente().getUuid());
                    preparedStatement.setObject(3, pedido.getEstabelecimento().getUuid());
                    preparedStatement.setString(4, pedido.getComentarioPedido());
                    preparedStatement.setBoolean(5, pedido.isEntrega());
                    preparedStatement.setBoolean(6, pedido.isCartao());
                    preparedStatement.setBoolean(7, pedido.isImpresso());
                    preparedStatement.setDouble(8, pedido.getValorPago());
                    preparedStatement.setDouble(9, pedido.getDesconto());
                    preparedStatement.setDouble(10, pedido.getPgCreditos());
                    preparedStatement.setDouble(11, pedido.getSubTotal());
                    preparedStatement.setDouble(12, pedido.getTotal());
                    preparedStatement.setTime(13, pedido.getHoraAgendamento());
                    preparedStatement.setString(14, pedido.getEstadoPedido().toString());
                    preparedStatement.setDouble(15, pedido.getTroco());
                    preparedStatement.setDouble(16, pedido.getTotalRemovido());
                    if (pedido.isEntrega()) {
                        preparedStatement.setString(17, pedido.getEndereco().getLogradouro());
                        preparedStatement.setString(18, pedido.getEndereco().getBairro());
                        preparedStatement.setString(19, pedido.getEndereco().getReferencia());
                        preparedStatement.setString(20, pedido.getEndereco().getNumero());
                    } else {
                        preparedStatement.setString(17, "");
                        preparedStatement.setString(18, "");
                        preparedStatement.setString(19, "");
                        preparedStatement.setString(20, "");
                    }
                    preparedStatement.setLong(21, pedido.getCod());
                    preparedStatement.setDouble(22, pedido.getTaxaEntrega());
                    preparedStatement.setObject(23, pedido.getTipoEntrega().getUuid());
                    preparedStatement.setTimestamp(24, pedido.getDataPedido());
                    preparedStatement.executeUpdate();
                    for (ItemPedido itemPedido : pedido.getProdutos()) {
                        itemPedido.calcularValor();
                        if (itemPedido.getPedido() == null) {
                            itemPedido.setPedido(pedido);
                        }
                        if (!ControleItensPedidos.getInstance().salvarItemPedido(connection, itemPedido)) {
                            throw new SQLException("Falha ao salvar item pedido");
                        }
                    }
                    connection.commit();
                    try {
                        SistemaDelivery sistemaDelivery = ControleSessions.getInstance().getSessionForEstabelecimento(pedido.getEstabelecimento());
                        if (sistemaDelivery.getBroadcaster() != null) {
                            sistemaDelivery.getBroadcaster().broadcast(sistemaDelivery.getSse().newEvent("novo-pedido", pedido.getUuid().toString()));
                        }
                    } catch (IOException e) {
                        throw e;
                    }
                    return true;
                } catch (SQLException | IOException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE \"Pedidos\"\n" +
                        "   SET  \"comentarioPedido\"=?, \n" +
                        "       entrega=?, cartao=?, impresso=?, \"valorPago\"=?, desconto=?, \"pgCreditos\"=?, \n" +
                        "       \"subTotal\"=?, total=?, \"horaAgendamento\"=?, \"estadoPedido\"=?, \n" +
                        "       troco=?, \"totalRemovido\"=?, logradouro=?, bairro=?, referencia=?, \n" +
                        "       numero=?, \"taxaEntrega\" = ?\n" +
                        " WHERE uuid=? and uuid_estabelecimento=?;\n")) {
                    pedido.calcularValor();
                    preparedStatement.setString(1, pedido.getComentarioPedido());
                    preparedStatement.setBoolean(2, pedido.isEntrega());
                    preparedStatement.setBoolean(3, pedido.isCartao());
                    preparedStatement.setBoolean(4, pedido.isImpresso());
                    preparedStatement.setDouble(5, pedido.getValorPago());
                    preparedStatement.setDouble(6, pedido.getDesconto());
                    preparedStatement.setDouble(7, pedido.getPgCreditos());
                    preparedStatement.setDouble(8, pedido.getSubTotal());
                    preparedStatement.setDouble(9, pedido.getTotal());
                    preparedStatement.setTime(10, pedido.getHoraAgendamento());
                    preparedStatement.setString(11, pedido.getEstadoPedido().toString());
                    preparedStatement.setDouble(12, pedido.getTroco());
                    preparedStatement.setDouble(13, pedido.getTotalRemovido());
                    if (pedido.isEntrega()) {
                        preparedStatement.setString(14, pedido.getEndereco().getLogradouro());
                        preparedStatement.setString(15, pedido.getEndereco().getBairro());
                        preparedStatement.setString(16, pedido.getEndereco().getReferencia());
                        preparedStatement.setString(17, pedido.getEndereco().getNumero());
                    } else {
                        preparedStatement.setString(14, "");
                        preparedStatement.setString(15, "");
                        preparedStatement.setString(16, "");
                        preparedStatement.setString(17, "");
                    }
                    preparedStatement.setDouble(18, pedido.getTaxaEntrega());
                    preparedStatement.setObject(19, pedido.getUuid());
                    preparedStatement.setObject(20, pedido.getEstabelecimento().getUuid());
                    if (preparedStatement.executeUpdate() != 1) {
                        throw new SQLException("Falha ao atualizar");
                    }
                    connection.commit();
                    if (pedidos.containsKey(pedido.getUuid())) {
                        Utilitarios.atualizarObjeto(pedidos.get(pedido.getUuid()), pedido);
                    }
                    return true;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        } catch (SQLException | IOException ex) {
            throw ex;
        }
    }

    public List<Pedido> getPedidosCliente(Cliente cliente) throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Pedidos\" where uuid_cliente = ?  order by \"dataPedido\" asc");
        ) {
            preparedStatement.setObject(1, cliente.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    pedidos.add(getPedidoByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return pedidos;
    }

    public List<Pedido> getPedidosDoDia(Estabelecimento estabelecimento) throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Pedidos\" where uuid_estabelecimento = ? and \"dataPedido\" >= ? order by \"dataPedido\" asc");
        ) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            preparedStatement.setTimestamp(2, new Timestamp(estabelecimento.getHoraAberturaPedidos().getTime()));
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    pedidos.add(getPedidoByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return pedidos;
    }

    public List<Pedido> getPedidosNaoImpressos(Estabelecimento estabelecimento) throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Pedidos\" where uuid_estabelecimento = ? and \"estadoPedido\"!='Cancelado' and \"estadoPedido\"!='Concluido' and not impresso order by \"dataPedido\" asc");
        ) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    pedidos.add(getPedidoByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return pedidos;
    }

    public List<Pedido> getPedidosAtivos(Estabelecimento estabelecimento) throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Pedidos\" where uuid_estabelecimento = ? and \"estadoPedido\"!=? and \"estadoPedido\"!=? order by \"dataPedido\" asc");
        ) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            preparedStatement.setString(2, EstadoPedido.Cancelado.toString());
            preparedStatement.setString(3, EstadoPedido.Concluido.toString());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    pedidos.add(getPedidoByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return pedidos;
    }

    public List<Pedido> getPedidosBetween(Estabelecimento estabelecimento, Date data1, Date data2) throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection connection = Conexao.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("select uuid from \"Pedidos\" where uuid_estabelecimento = ? and \"estadoPedido\"!='Cancelado' and \"dataPedido\" between ? and ?")
        ) {
            Calendar calendar = Calendar.getInstance();
            Calendar calendar2 = Calendar.getInstance();
            calendar.setTime(data1);
            calendar2.setTime(data2);
            preparedStatement.setObject(1, estabelecimento.getUuid());
            preparedStatement.setTimestamp(2, new Timestamp(data1.getTime()));
            preparedStatement.setTimestamp(3, new Timestamp(data2.getTime()));
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                pedidos.add(getPedidoByUUID(UUID.fromString(resultSet.getString("uuid"))));
            }
        } catch (SQLException ex) {
            throw ex;
        }
        return pedidos;
    }

    public List<Pedido> getPedidosDoMes(Estabelecimento estabelecimento) throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection connection = Conexao.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("select uuid from \"Pedidos\" where uuid_estabelecimento = ? and \"estadoPedido\"!='Cancelado' and EXTRACT(YEAR FROM \"dataPedido\") = EXTRACT(YEAR FROM current_timestamp) and EXTRACT(MONTH FROM \"dataPedido\") = EXTRACT(MONTH FROM current_timestamp)")
        ) {
            Calendar calendar = Calendar.getInstance();
            preparedStatement.setObject(1, estabelecimento.getUuid());
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                pedidos.add(getPedidoByUUID(UUID.fromString(resultSet.getString("uuid"))));
            }
        } catch (SQLException ex) {
            throw ex;
        }
        return pedidos;
    }

    public HashMap<String, Integer> getDadosDeliveryHoje(Estabelecimento estabelecimento) throws SQLException {
        HashMap<String, Integer> map = new HashMap<>();
        map.put("Concluido", 0);
        map.put("Novo", 0);
        map.put("Cancelado", 0);
        try (Connection connection = Conexao.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("select \"estadoPedido\",count(\"estadoPedido\") from \"Pedidos\" as a inner join \"Estabelecimentos\" as b on a.uuid_estabelecimento=b.uuid  where uuid_estabelecimento = ? and \"dataPedido\" >=\"horaAberturaPedidos\" group by \"estadoPedido\" ")) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                map.put(resultSet.getString(1), resultSet.getInt(2));
            }
        } catch (SQLException ex) {
            throw ex;
        }
        return map;
    }

    public HashMap<Integer, Integer> getEntregasPorHorario(Estabelecimento estabelecimento, Date data1) throws SQLException {
        HashMap<Integer, Integer> map = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(data1.getTime());
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        for (int x = 0; x < 24; x++) {
            map.put(x, 0);
        }
        try (Connection connection = Conexao.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("select extract(hour from \"dataPedido\") as hora,count(uuid) as total from \"Pedidos\" where uuid_estabelecimento = ? and \"estadoPedido\"!='Cancelado' and \"dataPedido\" between ? and ? group by extract(hour from \"dataPedido\")")) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            preparedStatement.setTimestamp(2, new Timestamp(data1.getTime()));
            preparedStatement.setTimestamp(3, new Timestamp(calendar.getTime().getTime()));
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                map.put(resultSet.getInt("hora"), resultSet.getInt("total"));
            }
        } catch (SQLException ex) {
            throw ex;
        }
        return map;
    }

    public HashMap<String, Integer> getEntregasPorDiaSemana(Estabelecimento estabelecimento, Date data1, Date data2) throws SQLException {
        HashMap<String, Integer> map = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        calendar.setTime(data1);
        calendar2.setTime(data2);
        for (int x = 1; x <= 7; x++) {
            map.put(Utilitarios.getDayOfWeekName(x), 0);
        }
        try (Connection connection = Conexao.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("select extract(isodow from \"dataPedido\") as diaSemana,count(uuid) as total from \"Pedidos\" where uuid_estabelecimento = ? and \"estadoPedido\"!='Cancelado' and \"dataPedido\" between ? and ? group by extract(isodow from \"dataPedido\")")) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            preparedStatement.setTimestamp(2, new Timestamp(data1.getTime()));
            preparedStatement.setTimestamp(3, new Timestamp(data2.getTime()));
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                map.put(Utilitarios.getDayOfWeekName(resultSet.getInt("diaSemana")), resultSet.getInt("total"));
            }
        } catch (SQLException ex) {
            throw ex;
        }
        return map;
    }

    public HashMap<String, LinkedHashMap<String, Double>> getReceitaPeriodo(Estabelecimento estabelecimento, Date data1, Date data2) throws SQLException {
        HashMap<String, LinkedHashMap<String, Double>> lista = new HashMap<>();
        LinkedHashMap<String, Double> map = new LinkedHashMap<>();
        LinkedHashMap<String, Double> map2 = new LinkedHashMap<>();
        LinkedHashMap<String, Double> map3 = new LinkedHashMap<>();
        lista.put("entrega", map);
        lista.put("retirada", map2);
        lista.put("total", map3);
        Calendar calendar = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        calendar.setTime(data1);
        calendar2.setTime(data2);
        LocalDate primeiroMesCopia = LocalDate.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
        LocalDate ultimoMes = LocalDate.of(calendar2.get(Calendar.YEAR), calendar2.get(Calendar.MONTH) + 1, calendar2.get(Calendar.DAY_OF_MONTH));
        if (primeiroMesCopia.isAfter(ultimoMes)) {
            throw new DateTimeException("Primeiro mês deve ser anterior ao ultimo Mês");
        }
        while (primeiroMesCopia.isBefore(ultimoMes)) {
            if (calendar.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)) {
                map.put(primeiroMesCopia.format(DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("pt-BR"))), 0.0);
                map2.put(primeiroMesCopia.format(DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("pt-BR"))), 0.0);
                map3.put(primeiroMesCopia.format(DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("pt-BR"))), 0.0);
            } else {
                map.put(primeiroMesCopia.format(DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("pt-BR"))) + "-" + primeiroMesCopia.format(DateTimeFormatter.ofPattern("yyyy")), 0.0);
                map2.put(primeiroMesCopia.format(DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("pt-BR"))) + "-" + primeiroMesCopia.format(DateTimeFormatter.ofPattern("yyyy")), 0.0);
                map3.put(primeiroMesCopia.format(DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("pt-BR"))) + "-" + primeiroMesCopia.format(DateTimeFormatter.ofPattern("yyyy")), 0.0);
            }
            primeiroMesCopia = primeiroMesCopia.plus(Period.ofMonths(1));
        }
        try (Connection connection = Conexao.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("select extract(year from \"dataPedido\") as ano,extract(month from \"dataPedido\") as mes,entrega,sum(total) as total from \"Pedidos\" where uuid_estabelecimento = ? and \"estadoPedido\"!='Cancelado' and \"dataPedido\" between ? and ?  group by extract(year from \"dataPedido\"),extract(month from \"dataPedido\"),entrega")) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            preparedStatement.setTimestamp(2, new Timestamp(data1.getTime()));
            preparedStatement.setTimestamp(3, new Timestamp(data2.getTime()));
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                if (calendar.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)) {
                    if (resultSet.getBoolean("entrega")) {
                        map.put(Utilitarios.getMonth(resultSet.getInt("mes")), resultSet.getDouble("total"));
                    } else {
                        map2.put(Utilitarios.getMonth(resultSet.getInt("mes")), resultSet.getDouble("total"));
                    }
                } else {
                    if (resultSet.getBoolean("entrega")) {
                        map.put(Utilitarios.getMonth(resultSet.getInt("mes")) + "-" + resultSet.getInt("ano"), resultSet.getDouble("total"));
                    } else {
                        map2.put(Utilitarios.getMonth(resultSet.getInt("mes")) + "-" + resultSet.getInt("ano"), resultSet.getDouble("total"));
                    }
                }
            }
        } catch (SQLException ex) {
            throw ex;
        }
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            map3.put(entry.getKey(), entry.getValue() + map2.get(entry.getKey()));
        }
        return lista;
    }

    public List<Pedido> getPedidosComProduto(Produto produto) throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection connection = Conexao.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("select distinct(b.uuid) from \"Items_Pedidos\" as a \n" +
                     "inner join \"Pedidos\" as b on a.uuid_pedido = b.uuid\n" +
                     "inner join \"Produtos\" as d on a.uuid_produto = d.uuid\n" +
                     "where b.uuid_estabelecimento = ? and d.uuid = ? AND \"estadoPedido\"!='Cancelado' and \"dataPedido\" >=date_trunc('MONTH',now())::DATE  ")) {
            preparedStatement.setObject(1, produto.getCategoria().getEstabelecimento().getUuid());
            preparedStatement.setObject(2, produto.getUuid());
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                pedidos.add(getPedidoByUUID(UUID.fromString(resultSet.getString("uuid"))));
            }
        } catch (SQLException ex) {
            throw ex;
        }
        return pedidos;
    }

    public List<Pedido> getPedidos(Estabelecimento estabelecimento) throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Pedidos\" where uuid_estabelecimento = ? order by \"dataPedido\" asc");
        ) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    pedidos.add(getPedidoByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return pedidos;
    }

}
