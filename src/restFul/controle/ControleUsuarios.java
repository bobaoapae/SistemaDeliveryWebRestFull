package restFul.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import restFul.modelo.LoginInUse;
import restFul.modelo.Usuario;
import sistemaDelivery.controle.ControleEstabelecimentos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ControleUsuarios {

    private static final Object syncronizeGetInstance = new Object();
    private static ControleUsuarios instance;
    private Map<UUID, Usuario> usuarios;

    private ControleUsuarios() {
        this.usuarios = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleUsuarios getInstance() {
        synchronized (syncronizeGetInstance) {
            if (instance == null) {
                instance = new ControleUsuarios();
            }
            return instance;
        }
    }

    public Usuario getUsuarioByUUID(UUID uuid) throws SQLException {
        if (usuarios.containsKey(uuid)) {
            return usuarios.get(uuid);
        }
        synchronized (usuarios) {
            try {
                QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
                ResultSetHandler<Usuario> h = new BeanHandler<Usuario>(Usuario.class);
                Usuario u = queryRunner.query("select * from \"Usuarios\" where uuid = ?", h, uuid);
                if (u == null) {
                    return null;
                }
                usuarios.putIfAbsent(uuid, u);
                u.setEstabelecimentos(ControleEstabelecimentos.getInstance().getEstabelecimentosUsuario(u));
                return usuarios.get(uuid);
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    public Usuario getUsuario(String login) throws SQLException {
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Usuarios\" where usuario = ?");
        ) {
            preparedStatement.setString(1, login);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                    return getUsuarioByUUID(uuid);
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return null;
    }

    public Usuario getUsuario(String login, String senha) throws SQLException {
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Usuarios\" where usuario = ? and senha = md5(?) ");
        ) {
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, senha);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                    return getUsuarioByUUID(uuid);
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return null;
    }

    public boolean salvarUsuario(Usuario usuario) throws LoginInUse, SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            if (usuario.getUuid() == null) {
                usuario.setUuid(UUID.randomUUID());
                if (getUsuario(usuario.getUsuario()) != null) {
                    connection.setAutoCommit(true);
                    throw new LoginInUse();
                }
                try (PreparedStatement preparedStatement = connection.prepareStatement("insert into \"Usuarios\" (uuid, uuid_usuario_indicacao, usuario, senha, \"tipoUsuario\", \"maxEstabelecimentos\")" +
                        "values (?,?,?,md5(?),?,?)")) {
                    preparedStatement.setObject(1, usuario.getUuid());
                    preparedStatement.setObject(2, usuario.getUuid_usuario_indicacao());
                    preparedStatement.setString(3, usuario.getUsuario());
                    preparedStatement.setString(4, usuario.getSenha());
                    preparedStatement.setString(5, usuario.getTipoUsuario().toString());
                    preparedStatement.setInt(6, usuario.getMaxEstabelecimentos());
                    preparedStatement.executeUpdate();
                    connection.commit();
                    return true;
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Usuarios\" set " +
                        "\"tipoUsuario\" = ?, \"maxEstabelecimentos\" = ?, senha = md5(?) where uuid=?")) {
                    preparedStatement.setString(1, usuario.getTipoUsuario().toString());
                    preparedStatement.setInt(2, usuario.getMaxEstabelecimentos());
                    preparedStatement.setString(3, usuario.getSenha());
                    preparedStatement.setObject(4, usuario.getUuid());
                    preparedStatement.executeUpdate();
                    connection.commit();
                    return true;
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        } catch (SQLException ex) {
            throw ex;
        }
    }

}
