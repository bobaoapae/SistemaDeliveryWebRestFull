package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import sistemaDelivery.modelo.Cliente;
import sistemaDelivery.modelo.Estabelecimento;
import sistemaDelivery.modelo.RecargaCliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ControleRecargas {

    private static ControleRecargas instace;
    private Map<UUID, RecargaCliente> recargas;

    private ControleRecargas() {
        this.recargas = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleRecargas getInstace() {
        if (instace == null) {
            instace = new ControleRecargas();
        }
        return instace;
    }

    public RecargaCliente getRecargaByUUID(UUID uuid) {
        if (recargas.containsKey(uuid)) {
            return recargas.get(uuid);
        }
        try {
            QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
            ResultSetHandler<RecargaCliente> h = new BeanHandler<RecargaCliente>(RecargaCliente.class);
            RecargaCliente recargaCliente = queryRunner.query("select * from \"Recargas_Clientes\" where uuid = ?", h, uuid);
            if (recargaCliente == null) {
                return null;
            }
            synchronized (recargas) {
                recargas.put(uuid, recargaCliente);
            }
            recargaCliente.setCliente(ControleClientes.getInstace().getClienteByUUID(recargaCliente.getUuid_cliente()));
            recargaCliente.setEstabelecimento(ControleEstabelecimentos.getInstace().getEstabelecimentoByUUID(recargaCliente.getUuid_estabelecimento()));
            return recargaCliente;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean salvarRecarga(RecargaCliente recargaCliente) {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("insert into \"Recargas_Clientes\" (uuid, uuid_cliente, uuid_estabelecimento, valor, \"tipoRecarga\") values (?,?,?,?,?)")) {
                recargaCliente.setUuid(UUID.randomUUID());
                preparedStatement.setObject(1, recargaCliente.getUuid());
                preparedStatement.setObject(2, recargaCliente.getCliente().getUuid());
                preparedStatement.setObject(3, recargaCliente.getEstabelecimento().getUuid());
                preparedStatement.setDouble(4, recargaCliente.getValor());
                preparedStatement.setString(5, recargaCliente.getTipoRecarga().toString());
                preparedStatement.executeUpdate();
                connection.commit();
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

    public List<RecargaCliente> getRecargasCliente(Cliente cliente, Estabelecimento estabelecimento) {
        List<RecargaCliente> recargaClientes = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select * from \"Recargas_Clientes\" where uuid_cliente = ? and uuid_estabelecimento = ?")) {
            preparedStatement.setObject(1, cliente.getUuid());
            preparedStatement.setObject(2, estabelecimento.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    recargaClientes.add(getRecargaByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return recargaClientes;
    }

}
