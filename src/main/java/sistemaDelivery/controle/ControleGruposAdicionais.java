package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import sistemaDelivery.modelo.Categoria;
import sistemaDelivery.modelo.GrupoAdicional;
import sistemaDelivery.modelo.Produto;
import utils.Utilitarios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ControleGruposAdicionais {

    private static final Object syncronizeGetInstance = new Object();
    private static ControleGruposAdicionais instance;
    private Map<UUID, GrupoAdicional> grupos;

    private ControleGruposAdicionais() {
        this.grupos = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleGruposAdicionais getInstance() {
        synchronized (syncronizeGetInstance) {
            if (instance == null) {
                instance = new ControleGruposAdicionais();
            }
            return instance;
        }
    }

    public GrupoAdicional getGrupoByUUID(UUID uuid) throws SQLException {
        if (grupos.containsKey(uuid)) {
            return grupos.get(uuid);
        }
        synchronized (grupos) {
            QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
            ResultSetHandler<GrupoAdicional> h = new BeanHandler<GrupoAdicional>(GrupoAdicional.class);
            GrupoAdicional grupo = queryRunner.query("select * from \"Grupos_Adicionais\" where uuid = ?", h, uuid);
            if (grupo == null) {
                return null;
            }
            grupos.putIfAbsent(uuid, grupo);
            grupo.setAdicionais(ControleAdicionais.getInstance().getAdicionaisGrupo(grupo));
            grupo.setCategoria(ControleCategorias.getInstance().getCategoriaByUUID(grupo.getUuid_categoria()));
            grupo.setProduto(ControleProdutos.getInstance().getProdutoByUUID(grupo.getUuid_produto()));
            return grupos.get(uuid);
        }
    }

    public boolean salvarGrupoAdicional(GrupoAdicional grupo) throws SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            if (grupo.getUuid() == null || this.getGrupoByUUID(grupo.getUuid()) == null) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("insert into \"Grupos_Adicionais\" (uuid, uuid_categoria, uuid_produto, \"nomeGrupo\", \"descricaoGrupo\", \"qtdMin\", \"qtdMax\", \"formaCobranca\") values(?,?,?,?,?,?,?,?)")) {
                    if (grupo.getUuid() == null) {
                        grupo.setUuid(UUID.randomUUID());
                    }
                    preparedStatement.setObject(1, grupo.getUuid());
                    preparedStatement.setObject(2, grupo.getUuid_categoria());
                    preparedStatement.setObject(3, grupo.getUuid_produto());
                    preparedStatement.setString(4, grupo.getNomeGrupo());
                    preparedStatement.setString(5, grupo.getDescricaoGrupo());
                    preparedStatement.setInt(6, grupo.getQtdMin());
                    preparedStatement.setInt(7, grupo.getQtdMax());
                    preparedStatement.setString(8, grupo.getFormaCobranca().toString());
                    preparedStatement.executeUpdate();
                    connection.commit();
                    if (grupo.getCategoria() != null) {
                        synchronized (grupo.getCategoria().getGruposAdicionais()) {
                            grupo.getCategoria().getGruposAdicionais().add(getGrupoByUUID(grupo.getUuid()));
                        }
                    } else if (grupo.getProduto() != null) {
                        synchronized (grupo.getProduto().getGruposAdicionais()) {
                            grupo.getProduto().getGruposAdicionais().add(getGrupoByUUID(grupo.getUuid()));
                        }
                    }
                    return true;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Grupos_Adicionais\" set \"nomeGrupo\" = ?, \"descricaoGrupo\" = ?, \"qtdMin\" = ?, \"qtdMax\" = ?, \"formaCobranca\" = ? where uuid = ?")) {
                    preparedStatement.setString(1, grupo.getNomeGrupo());
                    preparedStatement.setString(2, grupo.getDescricaoGrupo());
                    preparedStatement.setInt(3, grupo.getQtdMin());
                    preparedStatement.setInt(4, grupo.getQtdMax());
                    preparedStatement.setString(5, grupo.getFormaCobranca().toString());
                    preparedStatement.setObject(6, grupo.getUuid());
                    preparedStatement.executeUpdate();
                    connection.commit();
                    if (grupos.containsKey(grupo.getUuid())) {
                        Utilitarios.atualizarObjeto(grupos.get(grupo.getUuid()), grupo);
                    }
                    return true;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        }
    }

    public boolean excluirGrupo(GrupoAdicional grupoAdicional) throws SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Grupos_Adicionais\" set ativo = ? where uuid = ?")) {
                preparedStatement.setBoolean(1, false);
                preparedStatement.setObject(2, grupoAdicional.getUuid());
                preparedStatement.executeUpdate();
                connection.commit();
                if (grupoAdicional.getCategoria() != null) {
                    synchronized (grupoAdicional.getCategoria().getGruposAdicionais()) {
                        grupoAdicional.getCategoria().getGruposAdicionais().remove(grupoAdicional);
                    }
                } else if (grupoAdicional.getProduto() != null) {
                    synchronized (grupoAdicional.getProduto().getGruposAdicionais()) {
                        grupoAdicional.getProduto().getGruposAdicionais().remove(grupoAdicional);
                    }
                }
                synchronized (grupos) {
                    grupos.remove(grupoAdicional.getUuid());
                }
                return true;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<GrupoAdicional> getGruposCategoria(Categoria cat) throws SQLException {
        List<GrupoAdicional> grupos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Grupos_Adicionais\" where uuid_categoria = ? and ativo order by \"dataCriacao\" ");
        ) {
            preparedStatement.setObject(1, cat.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    grupos.add(getGrupoByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        }
        return grupos;
    }

    public List<GrupoAdicional> getGruposProduto(Produto produto) throws SQLException {
        List<GrupoAdicional> grupos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Grupos_Adicionais\" where uuid_produto = ? and ativo order by \"dataCriacao\" ");
        ) {
            preparedStatement.setObject(1, produto.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    grupos.add(getGrupoByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        }
        return grupos;
    }

}
