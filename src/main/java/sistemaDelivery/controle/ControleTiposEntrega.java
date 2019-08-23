package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import sistemaDelivery.modelo.Estabelecimento;
import sistemaDelivery.modelo.TipoEntrega;
import utils.Utilitarios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ControleTiposEntrega {

    private static final Object syncronizeGetInstance = new Object();
    private static ControleTiposEntrega instance;
    private Map<UUID, TipoEntrega> tiposEntrega;

    private ControleTiposEntrega() {
        this.tiposEntrega = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleTiposEntrega getInstance() {
        synchronized (syncronizeGetInstance) {
            if (instance == null) {
                instance = new ControleTiposEntrega();
            }
            return instance;
        }
    }

    public TipoEntrega getTipoEntregaByUUID(UUID uuid) throws SQLException {
        if (tiposEntrega.containsKey(uuid)) {
            return tiposEntrega.get(uuid);
        }
        synchronized (tiposEntrega) {
            QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
            ResultSetHandler<TipoEntrega> h = new BeanHandler<TipoEntrega>(TipoEntrega.class);
            TipoEntrega tipoEntrega = queryRunner.query("select * from \"TiposEntregas\" where uuid = ?", h, uuid);
            if (tipoEntrega == null) {
                return null;
            }
            tiposEntrega.putIfAbsent(uuid, tipoEntrega);
            tipoEntrega.setEstabelecimento(ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(tipoEntrega.getUuid_estabelecimento()));
            return tiposEntrega.get(uuid);
        }
    }

    public boolean salvarTipoEntrega(TipoEntrega tipoEntrega, Connection connection) throws SQLException {
        if (tipoEntrega.getUuid() == null || this.getTipoEntregaByUUID(tipoEntrega.getUuid()) == null) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("insert into \"TiposEntregas\"" +
                    " (uuid, uuid_estabelecimento, nome, valor, \"solicitarEndereco\")" +
                    "VALUES (?,?,?,?,?)")) {
                if (tipoEntrega.getUuid() == null) {
                    tipoEntrega.setUuid(UUID.randomUUID());
                }
                preparedStatement.setObject(1, tipoEntrega.getUuid());
                preparedStatement.setObject(2, tipoEntrega.getEstabelecimento().getUuid());
                preparedStatement.setString(3, tipoEntrega.getNome());
                preparedStatement.setDouble(4, tipoEntrega.getValor());
                preparedStatement.setBoolean(5, tipoEntrega.isSolicitarEndereco());
                preparedStatement.executeUpdate();
                return true;
            }
        } else {
            try (PreparedStatement preparedStatement = connection.prepareStatement("update \"TiposEntregas\" set " +
                    "nome=?,valor=?,\"solicitarEndereco\"=? where uuid = ? and uuid_estabelecimento = ?")) {
                preparedStatement.setString(1, tipoEntrega.getNome());
                preparedStatement.setDouble(2, tipoEntrega.getValor());
                preparedStatement.setBoolean(3, tipoEntrega.isSolicitarEndereco());
                preparedStatement.setObject(4, tipoEntrega.getUuid());
                preparedStatement.setObject(5, tipoEntrega.getEstabelecimento().getUuid());
                if (preparedStatement.executeUpdate() != 1) {
                    throw new SQLException("Falha ao atualizar");
                }
                if (tiposEntrega.containsKey(tipoEntrega.getUuid())) {
                    Utilitarios.atualizarObjeto(tiposEntrega.get(tipoEntrega.getUuid()), tipoEntrega);
                }
                return true;
            }
        }
    }

    public boolean salvarTipoEntrega(TipoEntrega tipoEntrega) throws SQLException {
        try (Connection connection = Conexao.getConnection();) {
            connection.setAutoCommit(false);
            if (tipoEntrega.getUuid() == null || this.getTipoEntregaByUUID(tipoEntrega.getUuid()) == null) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("insert into \"TiposEntregas\"" +
                        " (uuid, uuid_estabelecimento, nome, valor, \"solicitarEndereco\")" +
                        "VALUES (?,?,?,?,?)")) {
                    if (tipoEntrega.getUuid() == null) {
                        tipoEntrega.setUuid(UUID.randomUUID());
                    }
                    preparedStatement.setObject(1, tipoEntrega.getUuid());
                    preparedStatement.setObject(2, tipoEntrega.getEstabelecimento().getUuid());
                    preparedStatement.setString(3, tipoEntrega.getNome());
                    preparedStatement.setDouble(4, tipoEntrega.getValor());
                    preparedStatement.setBoolean(5, tipoEntrega.isSolicitarEndereco());
                    preparedStatement.executeUpdate();
                    connection.commit();
                    synchronized (tipoEntrega.getEstabelecimento().getTiposEntregas()) {
                        tipoEntrega.getEstabelecimento().getTiposEntregas().add(getTipoEntregaByUUID(tipoEntrega.getUuid()));
                    }
                    return true;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement("update \"TiposEntregas\" set " +
                        "nome=?,valor=?,\"solicitarEndereco\"=? where uuid = ? and uuid_estabelecimento = ?")) {
                    preparedStatement.setString(1, tipoEntrega.getNome());
                    preparedStatement.setDouble(2, tipoEntrega.getValor());
                    preparedStatement.setBoolean(3, tipoEntrega.isSolicitarEndereco());
                    preparedStatement.setObject(4, tipoEntrega.getUuid());
                    preparedStatement.setObject(5, tipoEntrega.getEstabelecimento().getUuid());
                    if (preparedStatement.executeUpdate() != 1) {
                        throw new SQLException("Falha ao atualizar");
                    }
                    connection.commit();
                    if (tiposEntrega.containsKey(tipoEntrega.getUuid())) {
                        Utilitarios.atualizarObjeto(tiposEntrega.get(tipoEntrega.getUuid()), tipoEntrega);
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

    public List<TipoEntrega> getTiposEntregasEstabelecimento(Estabelecimento es) throws SQLException {
        List<TipoEntrega> tipoEntregas = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"TiposEntregas\" where uuid_estabelecimento = ? and ativo order by valor asc, uuid desc ");
        ) {
            preparedStatement.setObject(1, es.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    tipoEntregas.add(getTipoEntregaByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        }
        return tipoEntregas;
    }

    public boolean excluirTipoEntrega(TipoEntrega tipoEntrega) throws SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("update \"TiposEntregas\" set ativo = ? where uuid = ? and uuid_estabelecimento = ?")) {
                preparedStatement.setBoolean(1, false);
                preparedStatement.setObject(2, tipoEntrega.getUuid());
                preparedStatement.setObject(3, tipoEntrega.getEstabelecimento().getUuid());
                if (preparedStatement.executeUpdate() != 1) {
                    throw new SQLException("Falha ao atualizar");
                }
                connection.commit();
                synchronized (tipoEntrega.getEstabelecimento().getTiposEntregas()) {
                    tipoEntrega.getEstabelecimento().getTiposEntregas().remove(tipoEntrega);
                }
                synchronized (tiposEntrega) {
                    if (tiposEntrega.containsKey(tipoEntrega.getUuid())) {
                        tiposEntrega.remove(tipoEntrega.getUuid());
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
    }

}
