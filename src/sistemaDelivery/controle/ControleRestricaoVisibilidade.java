package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import sistemaDelivery.modelo.Categoria;
import sistemaDelivery.modelo.Produto;
import sistemaDelivery.modelo.RestricaoVisibilidade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ControleRestricaoVisibilidade {

    private static final Object syncronizeGetInstance = new Object();
    private Map<UUID, RestricaoVisibilidade> restricoes;
    private static ControleRestricaoVisibilidade instance;

    private ControleRestricaoVisibilidade() {
        this.restricoes = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleRestricaoVisibilidade getInstance() {
        synchronized (syncronizeGetInstance) {
            if (instance == null) {
                instance = new ControleRestricaoVisibilidade();
            }
            return instance;
        }
    }

    public RestricaoVisibilidade getRestricaoByUUID(UUID uuid) {
        if (restricoes.containsKey(uuid)) {
            return restricoes.get(uuid);
        }
        synchronized (restricoes) {
            try (Connection connection = Conexao.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("select * from \"Restricoes_Visibilidade\" where uuid = ?")) {
                preparedStatement.setObject(1, uuid);
                ResultSet resultSet = preparedStatement.executeQuery();
                ResultSetHandler<RestricaoVisibilidade> h = new BeanHandler<RestricaoVisibilidade>(RestricaoVisibilidade.class);
                RestricaoVisibilidade restricao = h.handle(resultSet);
                if (restricao == null) {
                    return null;
                }
                boolean diasSemana[] = new boolean[7];
                diasSemana[0] = resultSet.getBoolean("domingo");
                diasSemana[1] = resultSet.getBoolean("segunda");
                diasSemana[2] = resultSet.getBoolean("terca");
                diasSemana[3] = resultSet.getBoolean("quarta");
                diasSemana[4] = resultSet.getBoolean("quinta");
                diasSemana[5] = resultSet.getBoolean("sexta");
                diasSemana[6] = resultSet.getBoolean("sabado");
                restricao.setDiasSemana(diasSemana);
                restricoes.putIfAbsent(uuid, restricao);
                restricao.setCategoria(ControleCategorias.getInstance().getCategoriaByUUID(restricao.getUuid_categoria()));
                restricao.setProduto(ControleProdutos.getInstance().getProdutoByUUID(restricao.getUuid_produto()));
                return restricoes.get(uuid);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public boolean salvarRestricao(Connection connection, RestricaoVisibilidade restricaoVisibilidade) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("insert into \"Restricoes_Visibilidade\" (uuid, uuid_categoria, uuid_produto, \"restricaoHorario\", \"restricaoDia\", \"horarioDe\", \"horarioAte\", domingo, segunda, terca, quarta, quinta, sexta, sabado) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                restricaoVisibilidade.setUuid(UUID.randomUUID());
                preparedStatement.setObject(1, restricaoVisibilidade.getUuid());
                if (restricaoVisibilidade.getCategoria() != null) {
                    preparedStatement.setObject(2, restricaoVisibilidade.getCategoria().getUuid());
                } else {
                    preparedStatement.setObject(2, null);
                }
                if (restricaoVisibilidade.getProduto() != null) {
                    preparedStatement.setObject(3, restricaoVisibilidade.getProduto().getUuid());
                } else {
                    preparedStatement.setObject(3, null);
                }
                preparedStatement.setBoolean(4, restricaoVisibilidade.isRestricaoHorario());
                preparedStatement.setBoolean(5, restricaoVisibilidade.isRestricaoDia());
                preparedStatement.setTime(6, restricaoVisibilidade.getHorarioDe());
                preparedStatement.setTime(7, restricaoVisibilidade.getHorarioAte());
                for (int x = 0; x < 7; x++) {
                    preparedStatement.setBoolean(x + 8, restricaoVisibilidade.getDiasSemana()[x]);
                }
                preparedStatement.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean excluirRestricao(Categoria cat) {
        return excluirRestricao(this.getRestricaoCategoria(cat));
    }

    public boolean excluirRestricao(Produto produto) {
        return excluirRestricao(this.getRestricaoProduto(produto));
    }

    public boolean excluirRestricao(RestricaoVisibilidade restricaoVisibilidade) {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("delete from \"Restricoes_Visibilidade\" where uuid = ?")) {
                preparedStatement.setObject(1, restricaoVisibilidade.getUuid());
                preparedStatement.executeUpdate();
                connection.commit();
                synchronized (restricoes) {
                    if (restricoes.containsKey(restricaoVisibilidade.getUuid())) {
                        restricoes.remove(restricaoVisibilidade.getUuid());
                    }
                }
                return true;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public RestricaoVisibilidade getRestricaoCategoria(Categoria cat) {
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Restricoes_Visibilidade\" where uuid_categoria = ?");
        ) {
            preparedStatement.setObject(1, cat.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return getRestricaoByUUID(UUID.fromString(resultSet.getString("uuid")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public RestricaoVisibilidade getRestricaoProduto(Produto produto) {
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Restricoes_Visibilidade\" where uuid_produto = ?");
        ) {
            preparedStatement.setObject(1, produto.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return getRestricaoByUUID(UUID.fromString(resultSet.getString("uuid")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
