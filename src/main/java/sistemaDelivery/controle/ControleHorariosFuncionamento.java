package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.quartz.SchedulerException;
import restFul.controle.ControleSessions;
import restFul.controle.ControleSistema;
import sistemaDelivery.modelo.Estabelecimento;
import sistemaDelivery.modelo.HorarioFuncionamento;
import utils.Utilitarios;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.*;
import java.util.logging.Level;

public class ControleHorariosFuncionamento {

    private static final Object syncronizeGetInstance = new Object();
    private static ControleHorariosFuncionamento instance;
    private Map<UUID, HorarioFuncionamento> horariosFuncionamento;

    private ControleHorariosFuncionamento() {
        this.horariosFuncionamento = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleHorariosFuncionamento getInstance() {
        synchronized (syncronizeGetInstance) {
            if (instance == null) {
                instance = new ControleHorariosFuncionamento();
            }
            return instance;
        }
    }

    public HorarioFuncionamento getHorarioFuncionamentoByUUID(UUID uuid) throws SQLException {
        if (horariosFuncionamento.containsKey(uuid)) {
            return horariosFuncionamento.get(uuid);
        }
        synchronized (horariosFuncionamento) {
            QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
            ResultSetHandler<HorarioFuncionamento> h = new BeanHandler<HorarioFuncionamento>(HorarioFuncionamento.class);
            HorarioFuncionamento horarioFuncionamento = queryRunner.query("select * from \"Horarios_Funcionamento\" where uuid = ?", h, uuid);
            if (horarioFuncionamento == null) {
                return null;
            }
            horariosFuncionamento.putIfAbsent(uuid, horarioFuncionamento);
            horarioFuncionamento.setEstabelecimento(ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(horarioFuncionamento.getUuid_estabelecimento()));
            return horariosFuncionamento.get(uuid);
        }
    }


    public boolean salvarHorarioFuncionamento(HorarioFuncionamento horarioFuncionamento) throws SQLException, SchedulerException, IOException {
        if (horarioFuncionamento.getHoraAbrir().isAfter(horarioFuncionamento.getHoraFechar())) {
            return false;
        }
        try (Connection connection = Conexao.getConnection();) {
            connection.setAutoCommit(false);
            if (horarioFuncionamento.getUuid() == null || this.getHorarioFuncionamentoByUUID(horarioFuncionamento.getUuid()) == null) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("insert into \"Horarios_Funcionamento\" (uuid, uuid_estabelecimento, \"diaDaSemana\", \"horaAbrir\", \"horaFechar\")" +
                        "values (?,?,?,?,?)")) {
                    if (horarioFuncionamento.getUuid() == null) {
                        horarioFuncionamento.setUuid(UUID.randomUUID());
                    }
                    preparedStatement.setObject(1, horarioFuncionamento.getUuid());
                    preparedStatement.setObject(2, horarioFuncionamento.getEstabelecimento().getUuid());
                    preparedStatement.setString(3, horarioFuncionamento.getDiaDaSemana().toString());
                    preparedStatement.setObject(4, horarioFuncionamento.getHoraAbrir());
                    preparedStatement.setObject(5, horarioFuncionamento.getHoraFechar());
                    preparedStatement.executeUpdate();
                    connection.commit();
                    horarioFuncionamento.getEstabelecimento().addHorarioFuncionamento(getHorarioFuncionamentoByUUID(horarioFuncionamento.getUuid()));
                    ControleSessions.getInstance().getSessionForEstabelecimento(horarioFuncionamento.getEstabelecimento()).atualizarJobsHorariosFuncionamento();
                    return true;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Horarios_Funcionamento\" set \"diaDaSemana\" = ?,\"horaAbrir\" = ?,\"horaFechar\" = ?,ativo = ? where uuid = ? and uuid_estabelecimento = ?")) {
                    preparedStatement.setString(1, horarioFuncionamento.getDiaDaSemana().toString());
                    preparedStatement.setObject(2, horarioFuncionamento.getHoraAbrir());
                    preparedStatement.setObject(3, horarioFuncionamento.getHoraFechar());
                    preparedStatement.setBoolean(4, horarioFuncionamento.isAtivo());
                    preparedStatement.setObject(5, horarioFuncionamento.getUuid());
                    preparedStatement.setObject(6, horarioFuncionamento.getEstabelecimento().getUuid());
                    if (preparedStatement.executeUpdate() != 1) {
                        throw new SQLException("Falha ao atualizar");
                    }
                    connection.commit();
                    if (horariosFuncionamento.containsKey(horarioFuncionamento.getUuid())) {
                        Utilitarios.atualizarObjeto(horariosFuncionamento.get(horarioFuncionamento.getUuid()), horarioFuncionamento);
                    }
                    horarioFuncionamento.getEstabelecimento().setHorariosFuncionamento(ControleHorariosFuncionamento.getInstance().getHorariosFuncionamento(horarioFuncionamento.getEstabelecimento()));
                    ControleSessions.getInstance().getSessionForEstabelecimento(horarioFuncionamento.getEstabelecimento()).atualizarJobsHorariosFuncionamento();
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

    public Map<DayOfWeek, List<HorarioFuncionamento>> getHorariosFuncionamento(Estabelecimento es) throws SQLException {
        Map<DayOfWeek, List<HorarioFuncionamento>> horarios = new HashMap<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Horarios_Funcionamento\" where uuid_estabelecimento = ? order by \"diaDaSemana\" asc ,\"horaAbrir\" asc");
        ) {
            preparedStatement.setObject(1, es.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    HorarioFuncionamento horarioFuncionamento = getHorarioFuncionamentoByUUID(UUID.fromString(resultSet.getString("uuid")));
                    if (!horarios.containsKey(horarioFuncionamento.getDiaDaSemana())) {
                        horarios.put(horarioFuncionamento.getDiaDaSemana(), Collections.synchronizedList(new ArrayList<>()));
                    }
                    horarios.get(horarioFuncionamento.getDiaDaSemana()).add(horarioFuncionamento);
                }
            }
        }
        return horarios;
    }

    public boolean excluirHorarioFuncionamento(HorarioFuncionamento horarioFuncionamento) throws SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("delete from \"Horarios_Funcionamento\" where uuid = ? and uuid_estabelecimento = ?")) {
                preparedStatement.setObject(1, horarioFuncionamento.getUuid());
                preparedStatement.setObject(2, horarioFuncionamento.getEstabelecimento().getUuid());
                if (preparedStatement.executeUpdate() != 1) {
                    throw new SQLException("Falha ao atualizar");
                }
                connection.commit();
                synchronized (horarioFuncionamento.getEstabelecimento().getHorariosFuncionamento(horarioFuncionamento.getDiaDaSemana())) {
                    horarioFuncionamento.getEstabelecimento().getHorariosFuncionamento(horarioFuncionamento.getDiaDaSemana()).remove(horarioFuncionamento);
                }
                synchronized (horariosFuncionamento) {
                    if (horariosFuncionamento.containsKey(horarioFuncionamento.getUuid())) {
                        horariosFuncionamento.remove(horarioFuncionamento.getUuid());
                    }
                }
                try {
                    ControleSessions.getInstance().getSessionForEstabelecimento(horarioFuncionamento.getEstabelecimento()).atualizarJobsHorariosFuncionamento();
                } catch (Exception ex) {
                    ControleSistema.getInstance().getLogger().log(Level.SEVERE, ex.getMessage(), ex);
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
