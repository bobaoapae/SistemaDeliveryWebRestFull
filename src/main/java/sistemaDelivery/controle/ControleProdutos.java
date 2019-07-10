package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import sistemaDelivery.modelo.Categoria;
import sistemaDelivery.modelo.Estabelecimento;
import sistemaDelivery.modelo.Produto;
import utils.Utilitarios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ControleProdutos {

    private static final Object syncronizeGetInstance = new Object();
    private static ControleProdutos instance;
    private Map<UUID, Produto> produtos;

    private ControleProdutos() {
        this.produtos = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleProdutos getInstance() {
        synchronized (syncronizeGetInstance) {
            if (instance == null) {
                instance = new ControleProdutos();
            }
            return instance;
        }
    }

    public Produto getProdutoByUUID(UUID uuid) throws SQLException {
        if (produtos.containsKey(uuid)) {
            return produtos.get(uuid);
        }
        synchronized (produtos) {
            try {
                QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
                ResultSetHandler<Produto> h = new BeanHandler<Produto>(Produto.class);
                Produto produto = queryRunner.query("select * from \"Produtos\" where uuid = ?", h, uuid);
                if (produto == null) {
                    return null;
                }
                produtos.putIfAbsent(uuid, produto);
                produto.setCategoria(ControleCategorias.getInstance().getCategoriaByUUID(produto.getUuid_categoria()));
                produto.setRestricaoVisibilidade(ControleRestricaoVisibilidade.getInstance().getRestricaoProduto(produto));
                produto.setGruposAdicionais(ControleGruposAdicionais.getInstance().getGruposProduto(produto));
                return produtos.get(uuid);
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    public boolean salvarProduto(Produto prod) throws SQLException {
        try (Connection connection = Conexao.getConnection();) {
            connection.setAutoCommit(false);
            if (prod.getUuid() == null || this.getProdutoByUUID(prod.getUuid()) == null) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO \"Produtos\"(\n" +
                        "            uuid, uuid_categoria, nome, descricao, foto, valor, \"onlyLocal\")\n" +
                        "    VALUES (?, ?, ?, ?, ?, ?,?);\n")) {
                    if (prod.getUuid() == null) {
                        prod.setUuid(UUID.randomUUID());
                    }
                    preparedStatement.setObject(1, prod.getUuid());
                    preparedStatement.setObject(2, prod.getCategoria().getUuid());
                    preparedStatement.setString(3, prod.getNome());
                    preparedStatement.setString(4, prod.getDescricao());
                    preparedStatement.setString(5, prod.getFoto());
                    preparedStatement.setDouble(6, prod.getValor());
                    preparedStatement.setBoolean(7, prod.isOnlyLocal());
                    preparedStatement.executeUpdate();
                    if (prod.getRestricaoVisibilidade() != null) {
                        prod.getRestricaoVisibilidade().setProduto(prod);
                        if (!ControleRestricaoVisibilidade.getInstance().salvarRestricao(connection, prod.getRestricaoVisibilidade())) {
                            throw new SQLException("Falha ao salvar restrição");
                        }
                    }
                    connection.commit();
                    Categoria cat = ControleCategorias.getInstance().getCategoriaByUUID(prod.getCategoria().getUuid());
                    synchronized (cat.getProdutos()) {
                        cat.getProdutos().add(ControleProdutos.getInstance().getProdutoByUUID(prod.getUuid()));
                    }
                    return true;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE \"Produtos\"\n" +
                        "set nome=?, descricao=?, foto = ?, valor=?, \"onlyLocal\"=?, \n" +
                        "      visivel = ? \n" +
                        " WHERE uuid = ? ;\n")) {
                    preparedStatement.setString(1, prod.getNome());
                    preparedStatement.setString(2, prod.getDescricao());
                    preparedStatement.setString(3, prod.getFoto());
                    preparedStatement.setDouble(4, prod.getValor());
                    preparedStatement.setBoolean(5, prod.isOnlyLocal());
                    preparedStatement.setBoolean(6, prod.isVisivel());
                    preparedStatement.setObject(7, prod.getUuid());
                    preparedStatement.executeUpdate();
                    if (ControleRestricaoVisibilidade.getInstance().getRestricaoProduto(prod) != null && !ControleRestricaoVisibilidade.getInstance().excluirRestricao(prod)) {
                        throw new SQLException("Falha ao remover restrição");
                    }
                    if (prod.getRestricaoVisibilidade() != null) {
                        prod.getRestricaoVisibilidade().setProduto(prod);
                        if (!ControleRestricaoVisibilidade.getInstance().salvarRestricao(connection, prod.getRestricaoVisibilidade())) {
                            throw new SQLException("Falha ao salvar restrição");
                        }
                    }
                    connection.commit();
                    if (produtos.containsKey(prod.getUuid())) {
                        Utilitarios.atualizarObjeto(produtos.get(prod.getUuid()), prod);
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

    public boolean excluirProduto(Produto prod) throws SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Produtos\" set ativo = ? where uuid = ? and uuid_categoria = ?")) {
                preparedStatement.setBoolean(1, false);
                preparedStatement.setObject(2, prod.getUuid());
                preparedStatement.setObject(3, prod.getCategoria().getUuid());
                if (preparedStatement.executeUpdate() != 1) {
                    throw new SQLException("Falha ao atualizar");
                }
                connection.commit();
                synchronized (prod.getCategoria().getProdutos()) {
                    prod.getCategoria().getProdutos().remove(prod);
                }
                synchronized (produtos) {
                    if (produtos.containsKey(prod.getUuid())) {
                        produtos.remove(prod.getUuid());
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


    public List<Produto> getProdutosCategoria(Categoria cat) throws SQLException {
        List<Produto> produtos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Produtos\" where uuid_categoria = ? and ativo order by \"dataCriacao\" ");
        ) {
            preparedStatement.setObject(1, cat.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    produtos.add(getProdutoByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return produtos;
    }

    public List<Produto> getProdutosEstabelecimento(Estabelecimento estabelecimento) throws SQLException {
        List<Produto> produtos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select a.uuid from \"Produtos\" as a inner join \"Categorias\" as b on a.uuid_categoria = b.uuid where b.uuid_estabelecimento = ? and a.ativo and b.ativo order by a.\"dataCriacao\" ");
        ) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    produtos.add(getProdutoByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return produtos;
    }

}
