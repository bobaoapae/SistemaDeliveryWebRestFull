package restFul.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import restFul.modelo.Token;
import sistemaDelivery.controle.ControleEstabelecimentos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ControleTokens {

    private static final Object syncronizeGetInstance = new Object();
    private Map<String, Token> tokens;
    private static ControleTokens instance;
    private ControleTokens() {
        this.tokens = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleTokens getInstance() {
        synchronized (syncronizeGetInstance) {
            if (instance == null) {
                instance = new ControleTokens();
            }
            return instance;
        }
    }

    public Token getToken(String token) throws SQLException {
        if (tokens.containsKey(token)) {
            return tokens.get(token);
        }
        synchronized (tokens) {
            try {
                QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
                ResultSetHandler<Token> h = new BeanHandler<Token>(Token.class);
                Token u = queryRunner.query("select * from \"Tokens\" where token = ?", h, token);
                if (u == null) {
                    return null;
                }
                tokens.putIfAbsent(token, u);
                u.setEstabelecimento(ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(u.getUuid_estabelecimento()));
                u.setUsuario(ControleUsuarios.getInstance().getUsuarioByUUID(u.getUuid_usuario()));
                return tokens.get(token);
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    public boolean saveToken(Token token) throws SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("insert into \"Tokens\" (token, uuid_estabelecimento, uuid_usuario, validade) values (?,?,?,?)")) {
                preparedStatement.setString(1, token.getToken());
                preparedStatement.setObject(2, token.getEstabelecimento().getUuid());
                preparedStatement.setObject(3, token.getUsuario().getUuid());
                preparedStatement.setDate(4, new java.sql.Date(token.getValidade().getTime()));
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
            throw ex;
        }
    }

    public boolean removerToken(Token token) throws SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("delete from \"Tokens\" where token = ? and uuid_estabelecimento = ? and uuid_usuario = ?")) {
                preparedStatement.setString(1, token.getToken());
                preparedStatement.setObject(2, token.getEstabelecimento().getUuid());
                preparedStatement.setObject(3, token.getUsuario().getUuid());
                preparedStatement.executeUpdate();
                connection.commit();
                synchronized (tokens) {
                    if (tokens.containsKey(token.getToken())) {
                        tokens.remove(token.getToken());
                    }
                }
                return true;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw ex;
        }
    }

}
