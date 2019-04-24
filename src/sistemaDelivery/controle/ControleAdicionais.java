package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import sistemaDelivery.modelo.AdicionalProduto;
import sistemaDelivery.modelo.GrupoAdicional;
import sistemaDelivery.modelo.ItemPedido;
import utils.Utilitarios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ControleAdicionais {

    private static final Object syncronizeGetInstance = new Object();
    private Map<UUID, AdicionalProduto> adicionais;
    private static ControleAdicionais instance;

    private ControleAdicionais() {
        this.adicionais = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleAdicionais getInstance() {
        synchronized (syncronizeGetInstance) {
            if (instance == null) {
                instance = new ControleAdicionais();
            }
            return instance;
        }
    }

    public AdicionalProduto getAdicionalByUUID(UUID uuid) throws SQLException {
        if (adicionais.containsKey(uuid)) {
            return adicionais.get(uuid);
        }
        synchronized (adicionais) {
            try {
                QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
                ResultSetHandler<AdicionalProduto> h = new BeanHandler<AdicionalProduto>(AdicionalProduto.class);
                AdicionalProduto adicional = queryRunner.query("select * from \"Adicionais\" where uuid = ?", h, uuid);
                if (adicional == null) {
                    return null;
                }
                adicionais.putIfAbsent(uuid, adicional);
                adicional.setGrupoAdicional(ControleGruposAdicionais.getInstance().getGrupoByUUID(adicional.getUuid_grupo_adicional()));
                return adicionais.get(uuid);
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    public boolean salvarAdicional(AdicionalProduto adicionalProduto) throws SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            if (adicionalProduto.getUuid() == null || this.getAdicionalByUUID(adicionalProduto.getUuid()) == null) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("insert into \"Adicionais\" (uuid, uuid_grupo_adicional, nome, descricao, valor) values (?,?,?,?,?)")) {
                    if (adicionalProduto.getUuid() == null) {
                        adicionalProduto.setUuid(UUID.randomUUID());
                    }
                    preparedStatement.setObject(1, adicionalProduto.getUuid());
                    preparedStatement.setObject(2, adicionalProduto.getGrupoAdicional().getUuid());
                    preparedStatement.setString(3, adicionalProduto.getNome());
                    preparedStatement.setString(4, adicionalProduto.getDescricao());
                    preparedStatement.setDouble(5, adicionalProduto.getValor());
                    preparedStatement.executeUpdate();
                    connection.commit();
                    synchronized (adicionalProduto.getGrupoAdicional().getAdicionais()) {
                        adicionalProduto.getGrupoAdicional().getAdicionais().add(getAdicionalByUUID(adicionalProduto.getUuid()));
                    }
                    return true;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Adicionais\" set nome = ?, descricao = ? , valor = ? where uuid = ?")) {
                    preparedStatement.setString(1, adicionalProduto.getNome());
                    preparedStatement.setString(2, adicionalProduto.getDescricao());
                    preparedStatement.setDouble(3, adicionalProduto.getValor());
                    preparedStatement.setObject(4, adicionalProduto.getUuid());
                    preparedStatement.executeUpdate();
                    connection.commit();
                    if (adicionais.containsKey(adicionalProduto.getUuid())) {
                        Utilitarios.atualizarObjeto(adicionais.get(adicionalProduto.getUuid()), adicionalProduto);
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
            throw ex;
        }
    }

    public boolean excluirAdicional(AdicionalProduto adicionalProduto) throws SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Adicionais\" set ativo = ? where uuid = ?")) {
                preparedStatement.setBoolean(1, false);
                preparedStatement.setObject(2, adicionalProduto.getUuid());
                preparedStatement.executeUpdate();
                connection.commit();
                synchronized (adicionalProduto.getGrupoAdicional().getAdicionais()) {
                    adicionalProduto.getGrupoAdicional().getAdicionais().remove(adicionalProduto);
                }
                synchronized (adicionais) {
                    adicionais.remove(adicionalProduto.getUuid());
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


    public List<AdicionalProduto> getAdicionaisGrupo(GrupoAdicional grupo) throws SQLException {
        List<AdicionalProduto> adicionais = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Adicionais\" where uuid_grupo_adicional = ? and ativo order by \"dataCriacao\" ");
        ) {
            preparedStatement.setObject(1, grupo.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    adicionais.add(getAdicionalByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return adicionais;
    }

    public List<AdicionalProduto> getAdicionaisItemPedido(ItemPedido item) throws SQLException {
        List<AdicionalProduto> adicionais = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid_adicional from \"Adicionais_Items_Pedidos\" as a inner join \"Adicionais\" as b on a.uuid_adicional = b.uuid inner join \"Grupos_Adicionais\" as c on b.uuid_grupo_adicional = c.uuid where uuid_item_pedido = ? order by uuid_produto asc,b.nome asc");
        ) {
            preparedStatement.setObject(1, item.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    adicionais.add(getAdicionalByUUID(UUID.fromString(resultSet.getString("uuid_adicional"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return adicionais;
    }

}
