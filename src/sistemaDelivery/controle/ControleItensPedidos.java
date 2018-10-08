package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import sistemaDelivery.modelo.AdicionalProduto;
import sistemaDelivery.modelo.ItemPedido;
import sistemaDelivery.modelo.Pedido;
import utils.Utilitarios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ControleItensPedidos {

    private static ControleItensPedidos instace;
    private Map<UUID, ItemPedido> itensPedidos;
    private static final Object syncronizeGetSession = new Object();

    private ControleItensPedidos() {
        this.itensPedidos = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleItensPedidos getInstace() {
        synchronized (syncronizeGetSession) {
            if (instace == null) {
                instace = new ControleItensPedidos();
            }
            return instace;
        }
    }

    public ItemPedido getItemByUUID(UUID uuid) {
        if (itensPedidos.containsKey(uuid)) {
            return itensPedidos.get(uuid);
        }
        synchronized (itensPedidos) {
            try {
                QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
                ResultSetHandler<ItemPedido> h = new BeanHandler<ItemPedido>(ItemPedido.class);
                ItemPedido item = queryRunner.query("select * from \"Items_Pedidos\" where uuid = ?", h, uuid);
                if (item == null) {
                    return null;
                }
                itensPedidos.put(uuid, item);
                item.setProduto(ControleProdutos.getInstace().getProdutoByUUID(item.getUuid_produto()));
                item.setAdicionais(ControleAdicionais.getInstace().getAdicionaisItemPedido(item));
                item.setPedido(ControlePedidos.getInstace().getPedidoByUUID(item.getUuid_pedido()));
                return item;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public boolean salvarItemPedido(Connection connection, ItemPedido itemPedido) throws SQLException {
        if (itemPedido.getUuid() == null) {
            itemPedido.setUuid(UUID.randomUUID());
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO \"Items_Pedidos\"(\n" +
                    "            uuid, uuid_pedido, uuid_produto, comentario, qtd, \"subTotal\", \n" +
                    "            \"qtdPago\", \"valorPago\")\n" +
                    "    VALUES (?, ?, ?, ?, ?, ?, \n" +
                    "            ?, ?);\n")) {
                preparedStatement.setObject(1, itemPedido.getUuid());
                preparedStatement.setObject(2, itemPedido.getPedido().getUuid());
                preparedStatement.setObject(3, itemPedido.getProduto().getUuid());
                preparedStatement.setString(4, itemPedido.getComentario());
                preparedStatement.setInt(5, itemPedido.getQtd());
                preparedStatement.setDouble(6, itemPedido.getSubTotal());
                preparedStatement.setInt(7, itemPedido.getQtdPago());
                preparedStatement.setDouble(8, itemPedido.getValorPago());
                preparedStatement.executeUpdate();
                for (AdicionalProduto adicionalProduto : itemPedido.getAdicionais()) {
                    try (PreparedStatement preparedStatement2 = connection.prepareStatement("insert into \"Adicionais_Items_Pedidos\" (uuid_item_pedido, uuid_adicional) values(?,?)")) {
                        preparedStatement2.setObject(1, itemPedido.getUuid());
                        preparedStatement2.setObject(2, adicionalProduto.getUuid());
                        preparedStatement2.executeUpdate();
                    } catch (SQLException ex) {
                        throw ex;
                    }
                }
                return true;
            } catch (SQLException ex) {
                throw ex;
            }
        }
        return false;
    }

    public boolean salvarItemPedido(ItemPedido itemPedido) {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            if (itemPedido.getUuid() == null) {
                return this.salvarItemPedido(connection, itemPedido);
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Items_Pedidos\" set comentario = ?, qtd =?, \"subTotal\" = ?, \"qtdPago\" = ?, \"valorPago\" = ? where uuid = ?")) {
                    preparedStatement.setString(1, itemPedido.getComentario());
                    preparedStatement.setInt(2, itemPedido.getQtd());
                    preparedStatement.setDouble(3, itemPedido.getSubTotal());
                    preparedStatement.setInt(4, itemPedido.getQtdPago());
                    preparedStatement.setDouble(5, itemPedido.getValorPago());
                    preparedStatement.setObject(6, itemPedido.getUuid());
                    preparedStatement.executeUpdate();
                    connection.commit();
                    if (itensPedidos.containsKey(itemPedido.getUuid())) {
                        Utilitarios.atualizarObjeto(itensPedidos.get(itemPedido.getUuid()), itemPedido);
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

    public boolean excluirPedido(ItemPedido itemPedido) {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Items_Pedidos\" set removido = ? where uuid = ?")) {
                preparedStatement.setBoolean(1, true);
                preparedStatement.setObject(2, itemPedido.getUuid());
                preparedStatement.executeUpdate();
                connection.commit();
                itemPedido.setRemovido(true);
                if (itensPedidos.containsKey(itemPedido.getUuid())) {
                    Utilitarios.atualizarObjeto(itensPedidos.get(itemPedido.getUuid()), itemPedido);
                }
                return true;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public List<ItemPedido> getItensPedidos(Pedido pedido) {
        List<ItemPedido> itemPedidos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Items_Pedidos\" where uuid_pedido = ?");
        ) {
            preparedStatement.setObject(1, pedido.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    itemPedidos.add(getItemByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return itemPedidos;
    }


}
