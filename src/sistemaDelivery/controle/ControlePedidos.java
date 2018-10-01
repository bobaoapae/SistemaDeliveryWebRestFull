package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import sistemaDelivery.modelo.Cliente;
import sistemaDelivery.modelo.Estabelecimento;
import sistemaDelivery.modelo.Pedido;

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
        try {
            QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
            ResultSetHandler<Pedido> h = new BeanHandler<Pedido>(Pedido.class);
            Pedido pedido = queryRunner.query("select * from \"Pedidos\" where uuid = ?", h, uuid);
            if (pedido == null) {
                return null;
            }
            synchronized (pedidos) {
                pedidos.put(uuid, pedido);
            }
            pedido.setEstabelecimento(ControleEstabelecimentos.getInstace().getEstabelecimentoByUUID(pedido.getUuid_estabelecimento()));
            pedido.setProdutos(ControleItensPedidos.getInstace().getItensPedidos(pedido));
            return pedido;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Pedido> getPedidosCliente(Cliente cliente, Estabelecimento estabelecimento) {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Pedidos\" where uuid_cliente = ? and uuid_estabelecimento=?");
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
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Pedidos\" where uuid_estabelecimento = ? and not impresso");
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
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Pedidos\" where uuid_estabelecimento = ? and \"estadoPedido\"!=? and \"estadoPedido\"!=?");
        ) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            preparedStatement.setObject(2, Pedido.EstadoPedido.Cancelado);
            preparedStatement.setObject(3, Pedido.EstadoPedido.Concluido);
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
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Pedidos\" where uuid_estabelecimento = ?");
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
