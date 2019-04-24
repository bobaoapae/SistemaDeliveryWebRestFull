package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import sistemaDelivery.modelo.Categoria;
import sistemaDelivery.modelo.Estabelecimento;
import utils.Utilitarios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ControleCategorias {

    private static final Object syncronizeGetInstance = new Object();
    private Map<UUID, Categoria> categorias;
    private static ControleCategorias instance;

    private ControleCategorias() {
        this.categorias = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleCategorias getInstance() {
        synchronized (syncronizeGetInstance) {
            if (instance == null) {
                instance = new ControleCategorias();
            }
            return instance;
        }
    }

    public Categoria getCategoriaByUUID(UUID uuid) throws SQLException {
        if (categorias.containsKey(uuid)) {
            return categorias.get(uuid);
        }
        synchronized (categorias) {
            try {
                QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
                ResultSetHandler<Categoria> h = new BeanHandler<Categoria>(Categoria.class);
                Categoria cat = queryRunner.query("select * from \"Categorias\" where uuid = ?", h, uuid);
                if (cat == null) {
                    return null;
                }
                categorias.putIfAbsent(uuid, cat);
                cat.setEstabelecimento(ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(cat.getUuid_estabelecimento()));
                cat.setProdutos(ControleProdutos.getInstance().getProdutosCategoria(cat));
                if (cat.isPrecisaPedirOutraCategoria()) {
                    cat.setCategoriasNecessarias(getCategoriasNecessariasEntrega(cat));
                }
                cat.setCategoriasFilhas(getCategoriasFilhas(cat));
                cat.setRestricaoVisibilidade(ControleRestricaoVisibilidade.getInstance().getRestricaoCategoria(cat));
                cat.setGruposAdicionais(ControleGruposAdicionais.getInstance().getGruposCategoria(cat));
                if (cat.getUuid_categoria_pai() != null) {
                    cat.setCategoriaPai(this.getCategoriaByUUID(cat.getUuid_categoria_pai()));
                }
                return categorias.get(uuid);
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    public boolean salvarCategoria(Categoria cat) throws SQLException {
        try (Connection connection = Conexao.getConnection();) {
            connection.setAutoCommit(false);
            if (cat.getUuid() == null || this.getCategoriaByUUID(cat.getUuid()) == null) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("insert into \"Categorias\"" +
                        " (uuid, uuid_estabelecimento, uuid_categoria_pai, \"nomeCategoria\", \"exemplosComentarioPedido\", " +
                        "\"qtdMinEntrega\", \"ordemExibicao\", \"fazEntrega\", \"precisaPedirOutraCategoria\", \"entregaGratis\") " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                    if (cat.getUuid() == null) {
                        cat.setUuid(UUID.randomUUID());
                    }
                    preparedStatement.setObject(1, cat.getUuid());
                    preparedStatement.setObject(2, cat.getEstabelecimento().getUuid());
                    if (cat.getCategoriaPai() != null) {
                        preparedStatement.setObject(3, cat.getCategoriaPai().getUuid());
                    } else {
                        preparedStatement.setObject(3, null);
                    }
                    preparedStatement.setString(4, cat.getNomeCategoria());
                    preparedStatement.setString(5, cat.getExemplosComentarioPedido());
                    preparedStatement.setInt(6, cat.getQtdMinEntrega());
                    preparedStatement.setInt(7, cat.getOrdemExibicao());
                    preparedStatement.setBoolean(8, cat.isFazEntrega());
                    preparedStatement.setBoolean(9, cat.isPrecisaPedirOutraCategoria());
                    preparedStatement.setBoolean(10, cat.isEntregaGratis());
                    preparedStatement.executeUpdate();
                    if (cat.getRestricaoVisibilidade() != null) {
                        cat.getRestricaoVisibilidade().setCategoria(cat);
                        if (!ControleRestricaoVisibilidade.getInstance().salvarRestricao(connection, cat.getRestricaoVisibilidade())) {
                            throw new SQLException("Falha ao salvar restrição");
                        }
                    }
                    if (cat.getCategoriasNecessarias() != null) {
                        for (Categoria categoria : cat.getCategoriasNecessarias()) {
                            try (PreparedStatement preparedStatement1 = connection.prepareStatement("insert into \"Categorias_Necessarias_Entrega\" (uuid_categoria, uuid_categoria_necessaria) values (?,?)")) {
                                preparedStatement1.setObject(1, cat.getUuid());
                                preparedStatement1.setObject(2, categoria.getUuid());
                                preparedStatement1.executeUpdate();
                            } catch (SQLException ex) {
                                throw ex;
                            }
                        }
                    }
                    connection.commit();
                    if (cat.getUuid_categoria_pai() != null) {
                        this.getCategoriaByUUID(cat.getUuid_categoria_pai()).getCategoriasFilhas().add(this.getCategoriaByUUID(cat.getUuid()));
                    } else {
                        cat.getEstabelecimento().getCategorias().add(this.getCategoriaByUUID(cat.getUuid()));
                    }
                    return true;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Categorias\" set " +
                        " \"nomeCategoria\"=?,\"exemplosComentarioPedido\"=?,\"qtdMinEntrega\"=?,\"ordemExibicao\"=?,\"fazEntrega\"=?,\"precisaPedirOutraCategoria\"=?,\"entregaGratis\"=?, visivel = ? where uuid = ? and uuid_estabelecimento = ?")) {
                    preparedStatement.setString(1, cat.getNomeCategoria());
                    preparedStatement.setString(2, cat.getExemplosComentarioPedido());
                    preparedStatement.setInt(3, cat.getQtdMinEntrega());
                    preparedStatement.setInt(4, cat.getOrdemExibicao());
                    preparedStatement.setBoolean(5, cat.isFazEntrega());
                    preparedStatement.setBoolean(6, cat.isPrecisaPedirOutraCategoria());
                    preparedStatement.setBoolean(7, cat.isEntregaGratis());
                    preparedStatement.setBoolean(8, cat.isVisivel());
                    preparedStatement.setObject(9, cat.getUuid());
                    preparedStatement.setObject(10, cat.getEstabelecimento().getUuid());
                    if (preparedStatement.executeUpdate() != 1) {
                        throw new SQLException("Falha ao atualizar");
                    }
                    if (ControleRestricaoVisibilidade.getInstance().getRestricaoCategoria(cat) != null && !ControleRestricaoVisibilidade.getInstance().excluirRestricao(cat)) {
                        throw new SQLException("Falha ao remover restrição");
                    }
                    if (cat.getRestricaoVisibilidade() != null) {
                        cat.getRestricaoVisibilidade().setCategoria(cat);
                        if (!ControleRestricaoVisibilidade.getInstance().salvarRestricao(connection, cat.getRestricaoVisibilidade())) {
                            throw new SQLException("Falha ao salvar restrição");
                        }
                    }
                    try (PreparedStatement preparedStatement1 = connection.prepareStatement("delete from \"Categorias_Necessarias_Entrega\" where uuid_categoria = ?")) {
                        preparedStatement1.setObject(1, cat.getUuid());
                        preparedStatement1.executeUpdate();
                    } catch (SQLException ex) {
                        throw ex;
                    }
                    if (cat.getCategoriasNecessarias() != null) {
                        for (Categoria categoria : cat.getCategoriasNecessarias()) {
                            try (PreparedStatement preparedStatement1 = connection.prepareStatement("insert into \"Categorias_Necessarias_Entrega\" (uuid_categoria, uuid_categoria_necessaria) values (?,?)")) {
                                preparedStatement1.setObject(1, cat.getUuid());
                                preparedStatement1.setObject(2, categoria.getUuid());
                                preparedStatement1.executeUpdate();
                            } catch (SQLException ex) {
                                throw ex;
                            }
                        }
                    }
                    connection.commit();
                    if (categorias.containsKey(cat.getUuid())) {
                        Utilitarios.atualizarObjeto(categorias.get(cat.getUuid()), cat);
                        if (cat.getCategoriasNecessarias() == null) {
                            categorias.get(cat.getUuid()).setCategoriasNecessarias(new ArrayList<>());
                        } else {
                            categorias.get(cat.getUuid()).setCategoriasNecessarias(cat.getCategoriasNecessarias());
                        }
                        if (cat.getUuidsCategoriasNecessarias() == null) {
                            categorias.get(cat.getUuid()).setUuidsCategoriasNecessarias(new ArrayList<>());

                        } else {
                            categorias.get(cat.getUuid()).setUuidsCategoriasNecessarias(cat.getUuidsCategoriasNecessarias());
                        }
                    }
                    return true;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        } catch (SQLException e) {
            throw e;
        }
    }

    public boolean excluirCategoria(Categoria cat) throws SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Categorias\" set ativo = ? where uuid = ? and uuid_estabelecimento = ?")) {
                preparedStatement.setBoolean(1, false);
                preparedStatement.setObject(2, cat.getUuid());
                preparedStatement.setObject(3, cat.getEstabelecimento().getUuid());
                if (preparedStatement.executeUpdate() != 1) {
                    throw new SQLException("Falha ao atualizar");
                }
                connection.commit();
                if (cat.getCategoriaPai() != null) {
                    synchronized (cat.getCategoriaPai().getCategoriasFilhas()) {
                        cat.getCategoriaPai().getCategoriasFilhas().remove(cat);
                    }
                } else {
                    synchronized (cat.getEstabelecimento().getCategorias()) {
                        cat.getEstabelecimento().getCategorias().remove(cat);
                    }
                }
                synchronized (categorias) {
                    if (categorias.containsKey(cat.getUuid())) {
                        categorias.remove(cat.getUuid());
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
            throw e;
        }
    }

    public List<Categoria> getCategoriasFilhas(Categoria cat) throws SQLException {
        List<Categoria> categorias = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Categorias\" where uuid_categoria_pai = ? and ativo order by \"dataCriacao\" ");
        ) {
            preparedStatement.setObject(1, cat.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    categorias.add(getCategoriaByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return categorias;
    }

    public List<Categoria> getCategoriasEstabelecimento(Estabelecimento es) throws SQLException {
        List<Categoria> categorias = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Categorias\" where uuid_estabelecimento = ? and uuid_categoria_pai is null and ativo order by \"ordemExibicao\" asc, uuid desc ");
        ) {
            preparedStatement.setObject(1, es.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    categorias.add(getCategoriaByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return categorias;
    }

    public List<Categoria> getCategoriasNecessariasEntrega(Categoria cat) throws SQLException {
        List<Categoria> categorias = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid_categoria_necessaria from \"Categorias_Necessarias_Entrega\" where uuid_categoria = ?");
        ) {
            preparedStatement.setObject(1, cat.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    categorias.add(getCategoriaByUUID(UUID.fromString(resultSet.getString("uuid_categoria_necessaria"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return categorias;
    }

}
