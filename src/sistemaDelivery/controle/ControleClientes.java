package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import sistemaDelivery.modelo.Cliente;
import sistemaDelivery.modelo.Endereco;
import utils.Utilitarios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ControleClientes {

    private static ControleClientes instace;
    private Map<UUID, Cliente> clientes;

    private ControleClientes() {
        this.clientes = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleClientes getInstace() {
        if (instace == null) {
            instace = new ControleClientes();
        }
        return instace;
    }

    public Cliente getClienteByUUID(UUID uuid) {
        if (clientes.containsKey(uuid)) {
            return clientes.get(uuid);
        }
        try (Connection connection = Conexao.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("select * from \"Clientes\" where uuid = ?")) {
            preparedStatement.setObject(1, uuid);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                ResultSetHandler<Cliente> h = new BeanHandler<Cliente>(Cliente.class);
                ResultSetHandler<Endereco> h1 = new BeanHandler<Endereco>(Endereco.class);
                Cliente cliente = h.handle(resultSet);
                if (cliente == null) {
                    return null;
                }
                resultSet.first();
                cliente.setEndereco(h1.handle(resultSet));
                synchronized (clientes) {
                    clientes.put(uuid, cliente);
                }
                return cliente;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Cliente getClienteChatId(String chatid) {
        try (Connection connection = Conexao.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("select uuid from \"Clientes\" where \"chatId\" !='' and \"chatId\" = ?")) {
            preparedStatement.setString(1, chatid);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return getClienteByUUID(UUID.fromString(resultSet.getString("uuid")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean salvarCliente(Cliente cliente) {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            if (cliente.getUuid() == null || getClienteByUUID(cliente.getUuid()) == null) {
                if (cliente.getUuid() == null) {
                    cliente.setUuid(UUID.randomUUID());
                }
                try (PreparedStatement preparedStatement = connection.prepareStatement("insert into \"Clientes\" (uuid,\"chatId\", nome, \"telefoneMovel\", \"telefoneFixo\", \"dataAniversario\", logradouro, bairro,referencia,numero,\"cadastroRealizado\") values(?,?,?,?,?,?,?,?,?,?,?)")) {
                    preparedStatement.setObject(1, cliente.getUuid());
                    preparedStatement.setString(2, cliente.getChatId());
                    preparedStatement.setString(3, cliente.getNome());
                    preparedStatement.setString(4, cliente.getTelefoneMovel());
                    preparedStatement.setString(5, cliente.getTelefoneFixo());
                    preparedStatement.setDate(6, new java.sql.Date(cliente.getDataAniversario().getTime()));
                    preparedStatement.setString(7, cliente.getEndereco().getLogradouro());
                    preparedStatement.setString(8, cliente.getEndereco().getBairro());
                    preparedStatement.setString(9, cliente.getEndereco().getReferencia());
                    preparedStatement.setString(10, cliente.getEndereco().getNumero());
                    preparedStatement.setBoolean(11, cliente.isCadastroRealizado());
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
                try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Clientes\" set \"chatId\" = ?,nome = ?,\"telefoneMovel\" = ?, \"telefoneFixo\" = ?,\"dataAniversario\" = ?, logradouro = ?, bairro = ?, referencia = ?, numero = ?, \"cadastroRealizado\" = ? where uuid = ?")) {
                    preparedStatement.setString(1, cliente.getChatId());
                    preparedStatement.setString(2, cliente.getNome());
                    preparedStatement.setString(3, cliente.getTelefoneMovel());
                    preparedStatement.setString(4, cliente.getTelefoneFixo());
                    preparedStatement.setDate(5, new java.sql.Date(cliente.getDataAniversario().getTime()));
                    preparedStatement.setString(6, cliente.getEndereco().getLogradouro());
                    preparedStatement.setString(7, cliente.getEndereco().getBairro());
                    preparedStatement.setString(8, cliente.getEndereco().getReferencia());
                    preparedStatement.setString(9, cliente.getEndereco().getNumero());
                    preparedStatement.setBoolean(10, cliente.isCadastroRealizado());
                    preparedStatement.setObject(11, cliente.getUuid());
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
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }


    public List<Cliente> getClientes() {
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Clientes\"");
        ) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    clientes.add(getClienteByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return clientes;
    }

}
