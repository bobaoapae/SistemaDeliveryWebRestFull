package restFul.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
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

    public Usuario getUsuarioByUUID(UUID uuid) {
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
                e.printStackTrace();
            }
            return null;
        }
    }

    public Usuario getUsuario(String login, String senha) {
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
            e.printStackTrace();
        }
        return null;
    }

}
