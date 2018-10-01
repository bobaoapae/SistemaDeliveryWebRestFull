package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import sistemaDelivery.modelo.ItemPedido;
import sistemaDelivery.modelo.Pedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ControleItensPedidos {

    private static ControleItensPedidos instace;
    private Map<UUID, ItemPedido> itensPedidos;

    private ControleItensPedidos() {
        this.itensPedidos = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleItensPedidos getInstace() {
        if (instace == null) {
            instace = new ControleItensPedidos();
        }
        return instace;
    }

    public ItemPedido getItemByUUID(UUID uuid) {
        if (itensPedidos.containsKey(uuid)) {
            return itensPedidos.get(uuid);
        }
        try {
            QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
            ResultSetHandler<ItemPedido> h = new BeanHandler<ItemPedido>(ItemPedido.class);
            ItemPedido item = queryRunner.query("select * from \"Items_Pedidos\" where uuid = ?", h, uuid);
            if (item == null) {
                return null;
            }
            synchronized (itensPedidos) {
                itensPedidos.put(uuid, item);
            }
            item.setProduto(ControleProdutos.getInstace().getProdutoByUUID(item.getUuid_produto()));
            item.setAdicionais(ControleAdicionais.getInstace().getAdicionaisItemPedido(item));
            return item;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
