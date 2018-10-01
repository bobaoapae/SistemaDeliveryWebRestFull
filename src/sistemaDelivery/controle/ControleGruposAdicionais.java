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

    private static ControleGruposAdicionais instace;
    private Map<UUID, GrupoAdicional> grupos;

    private ControleGruposAdicionais() {
        this.grupos = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleGruposAdicionais getInstace() {
        if (instace == null) {
            instace = new ControleGruposAdicionais();
        }
        return instace;
    }

    public GrupoAdicional getGrupoByUUID(UUID uuid) {
        if (grupos.containsKey(uuid)) {
            return grupos.get(uuid);
        }
        try {
            QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
            ResultSetHandler<GrupoAdicional> h = new BeanHandler<GrupoAdicional>(GrupoAdicional.class);
            GrupoAdicional grupo = queryRunner.query("select * from \"Grupos_Adicionais\" where uuid = ?", h, uuid);
            if (grupo == null) {
                return null;
            }
            synchronized (grupos) {
                grupos.put(uuid, grupo);
            }
            grupo.setAdicionais(ControleAdicionais.getInstace().getAdicionaisGrupo(grupo));
            grupo.setCategoria(ControleCategorias.getInstace().getCategoriaByUUID(grupo.getUuid_categoria()));
            grupo.setProduto(ControleProdutos.getInstace().getProdutoByUUID(grupo.getUuid_produto()));
            return grupo;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean salvarGrupoAdicional(GrupoAdicional grupo) {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            if (grupo.getUuid() == null) {
                grupo.setUuid(UUID.randomUUID());
                try (PreparedStatement preparedStatement = connection.prepareStatement("insert into \"Grupos_Adicionais\" (uuid, uuid_categoria, uuid_produto, \"nomeGrupo\", \"descricaoGrupo\", \"qtdMin\", \"qtdMax\", \"formaCobranca\") values(?,?,?,?,?,?,?,?)")) {
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
                        grupo.getCategoria().getGruposAdicionais().add(grupo);
                    } else if (grupo.getProduto() != null) {
                        grupo.getProduto().getGruposAdicionais().add(grupo);
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
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean excluirGrupo(GrupoAdicional grupoAdicional) {
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
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public List<GrupoAdicional> getGruposCategoria(Categoria cat) {
        List<GrupoAdicional> grupos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Grupos_Adicionais\" where uuid_categoria = ? and ativo");
        ) {
            preparedStatement.setObject(1, cat.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    grupos.add(getGrupoByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return grupos;
    }

    public List<GrupoAdicional> getGruposProduto(Produto produto) {
        List<GrupoAdicional> grupos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Grupos_Adicionais\" where uuid_produto = ? and ativo");
        ) {
            preparedStatement.setObject(1, produto.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    grupos.add(getGrupoByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return grupos;
    }

}
