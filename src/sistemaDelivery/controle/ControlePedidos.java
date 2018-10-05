package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import sistemaDelivery.modelo.*;
import utils.PedidoHandlerRowProcessor;
import utils.Utilitarios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ControlePedidos {

    private static ControlePedidos instace;
    private Map<UUID, Pedido> pedidos;

    private ControlePedidos() {
        this.pedidos = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControlePedidos getInstace() {
        if (instace == null) {
            instace = new ControlePedidos();
        }
        return instace;
    }

    public Pedido getPedidoByUUID(UUID uuid) {
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
                pedidos.put(uuid, pedido);
                pedido.setCliente(ControleClientes.getInstace().getClienteByUUID(pedido.getUuid_cliente()));
                pedido.setEstabelecimento(ControleEstabelecimentos.getInstace().getEstabelecimentoByUUID(pedido.getUuid_estabelecimento()));
                pedido.setProdutos(ControleItensPedidos.getInstace().getItensPedidos(pedido));
                return pedido;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public boolean salvarPedido(Pedido pedido) {
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
                        "            troco, \"totalRemovido\", logradouro, bairro, referencia, numero,cod,\"taxaEntrega\")\n" +
                        "    VALUES (?, ?, ?, ?, \n" +
                        "            ?, ?, ?, ?, ?, ?, \n" +
                        "            ?, ?, ?, ?, ?, \n" +
                        "            ?, ?, ?, ?, ?, ?,?);\n")) {
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
                    preparedStatement.executeUpdate();
                    for (ItemPedido itemPedido : pedido.getProdutos()) {
                        itemPedido.calcularValor();
                        if (itemPedido.getPedido() == null) {
                            itemPedido.setPedido(pedido);
                        }
                        if (!ControleItensPedidos.getInstace().salvarItemPedido(connection, itemPedido)) {
                            throw new SQLException("Falha ao salvar item pedido");
                        }
                    }
                    connection.commit();
                    return true;
                } catch (SQLException ex) {
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
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public List<Pedido> getPedidosCliente(Cliente cliente, Estabelecimento estabelecimento) {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Pedidos\" where uuid_cliente = ? and uuid_estabelecimento=? order by \"dataPedido\" asc");
        ) {
            preparedStatement.setObject(1, cliente.getUuid());
            preparedStatement.setObject(2, estabelecimento.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    pedidos.add(getPedidoByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pedidos;
    }

    public List<Pedido> getPedidosNaoImpressos(Estabelecimento estabelecimento) {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Pedidos\" where uuid_estabelecimento = ? and not impresso order by \"dataPedido\" asc");
        ) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    pedidos.add(getPedidoByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pedidos;
    }

    public List<Pedido> getPedidosAtivos(Estabelecimento estabelecimento) {
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
            e.printStackTrace();
        }
        return pedidos;
    }

    public List<Pedido> getPedidos(Estabelecimento estabelecimento) {
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
            e.printStackTrace();
        }
        return pedidos;
    }

}
