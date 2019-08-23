package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import sistemaDelivery.modelo.Cliente;
import sistemaDelivery.modelo.Estabelecimento;
import utils.ClienteHandlerRowProcessor;
import utils.Utilitarios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ControleClientes {

    private static final Object syncronizeGetInstance = new Object();
    private static ControleClientes instance;
    private Map<UUID, Cliente> clientes;

    private ControleClientes() {
        this.clientes = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleClientes getInstance() {
        synchronized (syncronizeGetInstance) {
            if (instance == null) {
                instance = new ControleClientes();
            }
            return instance;
        }
    }

    public Cliente getClienteByUUID(UUID uuid) throws SQLException {
        if (clientes.containsKey(uuid)) {
            return clientes.get(uuid);
        }
        synchronized (clientes) {
            QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
            ResultSetHandler<Cliente> h = new BeanHandler<Cliente>(Cliente.class, new ClienteHandlerRowProcessor());
            Cliente cliente = queryRunner.query("select * from \"Clientes\" where uuid = ?", h, uuid);
            if (cliente == null) {
                return null;
            }
            clientes.putIfAbsent(uuid, cliente);
            cliente.setEstabelecimento(ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(cliente.getUuid_estabelecimento()));
            return clientes.get(uuid);
        }
    }

    public Cliente getClienteChatId(String chatid, Estabelecimento estabelecimento) throws SQLException {
        try (Connection connection = Conexao.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("select uuid from \"Clientes\" where \"chatId\" !='' and \"chatId\" = ? and uuid_estabelecimento = ?")) {
            preparedStatement.setString(1, chatid);
            preparedStatement.setObject(2, estabelecimento.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return getClienteByUUID(UUID.fromString(resultSet.getString("uuid")));
                }
            }
        }
        return null;
    }

    public boolean salvarCliente(Cliente cliente) throws SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            if (cliente.getUuid() == null || getClienteByUUID(cliente.getUuid()) == null) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("insert into \"Clientes\" (uuid,\"chatId\", nome, \"telefoneMovel\", \"telefoneFixo\", \"dataAniversario\", logradouro, bairro,referencia,numero,\"cadastroRealizado\",uuid_estabelecimento) values(?,?,?,?,?,?,?,?,?,?,?,?)")) {
                    if (cliente.getUuid() == null) {
                        cliente.setUuid(UUID.randomUUID());
                    }
                    preparedStatement.setObject(1, cliente.getUuid());
                    preparedStatement.setString(2, cliente.getChatId());
                    preparedStatement.setString(3, cliente.getNome());
                    preparedStatement.setString(4, cliente.getTelefoneMovel());
                    preparedStatement.setString(5, cliente.getTelefoneFixo());
                    preparedStatement.setObject(6, cliente.getDataAniversario());
                    if (cliente.getEndereco() != null) {
                        preparedStatement.setString(7, cliente.getEndereco().getLogradouro());
                        preparedStatement.setString(8, cliente.getEndereco().getBairro());
                        preparedStatement.setString(9, cliente.getEndereco().getReferencia());
                        preparedStatement.setString(10, cliente.getEndereco().getNumero());
                    } else {
                        preparedStatement.setString(7, "");
                        preparedStatement.setString(8, "");
                        preparedStatement.setString(9, "");
                        preparedStatement.setString(10, "");
                    }
                    preparedStatement.setBoolean(11, cliente.isCadastroRealizado());
                    preparedStatement.setObject(12, cliente.getEstabelecimento().getUuid());
                    preparedStatement.executeUpdate();
                    connection.commit();
                    return true;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Clientes\" set \"chatId\" = ?,nome = ?,\"telefoneMovel\" = ?, \"telefoneFixo\" = ?,\"dataAniversario\" = ?, logradouro = ?, bairro = ?, referencia = ?, numero = ?, \"cadastroRealizado\" = ? where uuid = ? and uuid_estabelecimento = ?")) {
                    preparedStatement.setString(1, cliente.getChatId());
                    preparedStatement.setString(2, cliente.getNome());
                    preparedStatement.setString(3, cliente.getTelefoneMovel());
                    preparedStatement.setString(4, cliente.getTelefoneFixo());
                    preparedStatement.setObject(5, cliente.getDataAniversario());
                    if (cliente.getEndereco() != null) {
                        preparedStatement.setString(6, cliente.getEndereco().getLogradouro());
                        preparedStatement.setString(7, cliente.getEndereco().getBairro());
                        preparedStatement.setString(8, cliente.getEndereco().getReferencia());
                        preparedStatement.setString(9, cliente.getEndereco().getNumero());
                    } else {
                        preparedStatement.setString(6, "");
                        preparedStatement.setString(7, "");
                        preparedStatement.setString(8, "");
                        preparedStatement.setString(9, "");
                    }
                    preparedStatement.setBoolean(10, cliente.isCadastroRealizado());
                    preparedStatement.setObject(11, cliente.getUuid());
                    preparedStatement.setObject(12, cliente.getEstabelecimento().getUuid());
                    preparedStatement.executeUpdate();
                    connection.commit();
                    if (clientes.containsKey(cliente.getUuid())) {
                        Utilitarios.atualizarObjeto(clientes.get(cliente.getUuid()), cliente);
                    }
                    return true;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                }
            }
        }
    }


    public List<Cliente> getClientes(Estabelecimento estabelecimento) throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Clientes\" where uuid_estabelecimento = ?");
        ) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    clientes.add(getClienteByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        }
        return clientes;
    }

    public List<Cliente> getClientesCompraramEstabelecimento(Estabelecimento estabelecimento) throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Clientes\" where uuid in (select uuid_cliente from \"Pedidos\" where uuid_estabelecimento = ?)");
        ) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    clientes.add(getClienteByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        }
        return clientes;
    }

}
