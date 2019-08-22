package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import sistemaDelivery.modelo.Estabelecimento;
import sistemaDelivery.modelo.Rodizio;
import utils.Utilitarios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ControleRodizios {

    private static final Object syncronizeGetSession = new Object();
    private static ControleRodizios instace;
    private Map<UUID, Rodizio> rodizios;

    private ControleRodizios() {
        this.rodizios = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleRodizios getInstace() {
        synchronized (syncronizeGetSession) {
            if (instace == null) {
                instace = new ControleRodizios();
            }
            return instace;
        }
    }

    public Rodizio getRodizioByUUID(UUID uuid) throws SQLException {
        if (rodizios.containsKey(uuid)) {
            return rodizios.get(uuid);
        }
        synchronized (rodizios) {
            try (Connection connection = Conexao.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement("select * from \"Rodizios\" where uuid = ?")) {
                preparedStatement.setObject(1, uuid);
                ResultSet resultSet = preparedStatement.executeQuery();
                ResultSetHandler<Rodizio> h = new BeanHandler<Rodizio>(Rodizio.class);
                Rodizio rodizio = h.handle(resultSet);
                if (rodizio == null) {
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
                rodizio.setDiasSemana(diasSemana);
                rodizios.putIfAbsent(uuid, rodizio);
                rodizio.setEstabelecimento(ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(rodizio.getUuid_estabelecimento()));
                return rodizios.get(uuid);
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    public boolean salvarRodizio(Rodizio rodizio) throws SQLException {
        try (Connection connection = Conexao.getConnection();) {
            connection.setAutoCommit(false);
            if (rodizio.getUuid() == null) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO \"Rodizios\" (uuid, uuid_estabelecimento, nome, descricao, valor, \"horaInicio\", domingo,segunda,terca,quarta,quinta,sexta,sabado) values (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                    rodizio.setUuid(UUID.randomUUID());
                    preparedStatement.setObject(1, rodizio.getUuid());
                    preparedStatement.setObject(2, rodizio.getEstabelecimento().getUuid());
                    preparedStatement.setString(3, rodizio.getNome());
                    preparedStatement.setString(4, rodizio.getDescricao());
                    preparedStatement.setDouble(5, rodizio.getValor());
                    preparedStatement.setObject(6, rodizio.getHoraInicio());
                    for (int x = 0; x < 7; x++) {
                        preparedStatement.setBoolean(x + 7, rodizio.getDiasSemana()[x]);
                    }
                    preparedStatement.executeUpdate();
                    connection.commit();
                    synchronized (rodizio.getEstabelecimento().getRodizios()) {
                        rodizio.getEstabelecimento().getRodizios().add(ControleRodizios.getInstace().getRodizioByUUID(rodizio.getUuid()));
                    }
                    return true;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE \"Rodizios\"\n" +
                        "set nome=?, descricao=?, valor=?, \"horaInicio\"=?, \n" +
                        "     domingo = ?, segunda = ?, terca = ?, quarta = ?, quinta = ?, sexta = ?, sabado = ?  \n" +
                        " WHERE uuid = ? and uuid_estabelecimento = ?;\n")) {
                    preparedStatement.setString(1, rodizio.getNome());
                    preparedStatement.setString(2, rodizio.getDescricao());
                    preparedStatement.setDouble(3, rodizio.getValor());
                    preparedStatement.setObject(4, rodizio.getHoraInicio());
                    for (int x = 0; x < 7; x++) {
                        preparedStatement.setBoolean(x + 5, rodizio.getDiasSemana()[x]);
                    }
                    preparedStatement.setObject(12, rodizio.getUuid());
                    preparedStatement.setObject(13, rodizio.getEstabelecimento().getUuid());
                    preparedStatement.executeUpdate();
                    connection.commit();
                    if (rodizios.containsKey(rodizio.getUuid())) {
                        Utilitarios.atualizarObjeto(rodizios.get(rodizio.getUuid()), rodizio);
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

    public boolean excluirRodizio(Rodizio rodizio) throws SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Rodizios\" set ativo = ? where uuid = ? and uuid_estabelecimento = ?")) {
                preparedStatement.setBoolean(1, false);
                preparedStatement.setObject(2, rodizio.getUuid());
                preparedStatement.setObject(3, rodizio.getEstabelecimento().getUuid());
                if (preparedStatement.executeUpdate() != 1) {
                    throw new SQLException("Falha ao atualizar");
                }
                connection.commit();
                synchronized (rodizio.getEstabelecimento().getRodizios()) {
                    rodizio.getEstabelecimento().getRodizios().remove(rodizio);
                }
                synchronized (rodizios) {
                    if (rodizios.containsKey(rodizio.getUuid())) {
                        rodizios.remove(rodizio.getUuid());
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

    public List<Rodizio> getRodiziosEstabelecimento(Estabelecimento estabelecimento) throws SQLException {
        List<Rodizio> rodizios = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Rodizios\" where uuid_estabelecimento = ? and ativo order by \"dataCriacao\" ");
        ) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    rodizios.add(getRodizioByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return rodizios;
    }

}
