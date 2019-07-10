package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import restFul.controle.ControleSessions;
import sistemaDelivery.SistemaDelivery;
import sistemaDelivery.modelo.Cliente;
import sistemaDelivery.modelo.Estabelecimento;
import sistemaDelivery.modelo.Reserva;
import utils.Utilitarios;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ControleReservas {

    private static final Object syncronizeGetInstance = new Object();
    private static ControleReservas instance;
    private Map<UUID, Reserva> reservas;

    private ControleReservas() {
        this.reservas = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleReservas getInstance() {
        synchronized (syncronizeGetInstance) {
            if (instance == null) {
                instance = new ControleReservas();
            }
            return instance;
        }
    }

    public Reserva getReservaByUUID(UUID uuid) throws SQLException {
        if (reservas.containsKey(uuid)) {
            return reservas.get(uuid);
        }
        synchronized (reservas) {
            try {
                QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
                ResultSetHandler<Reserva> h = new BeanHandler<Reserva>(Reserva.class);
                Reserva reserva = queryRunner.query("select * from \"Reservas\" where uuid = ?", h, uuid);
                if (reserva == null) {
                    return null;
                }
                reservas.putIfAbsent(uuid, reserva);
                reserva.setCliente(ControleClientes.getInstance().getClienteByUUID(reserva.getUuid_cliente()));
                reserva.setEstabelecimento(ControleEstabelecimentos.getInstance().getEstabelecimentoByUUID(reserva.getUuid_estabelecimento()));
                return reservas.get(uuid);
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    public boolean salvarReserva(Reserva reserva) throws IOException, SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            if (reserva.getUuid() == null) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("select next_cod_reserva_estabelecimento(?)")) {
                    preparedStatement.setObject(1, reserva.getEstabelecimento().getUuid());
                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.next()) {
                        reserva.setCod(resultSet.getLong(1));
                    } else {
                        return false;
                    }
                }
                try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO public.\"Reservas\"(\n" +
                        "            uuid, uuid_estabelecimento, uuid_cliente, \"dataReserva\", \"telefoneContato\", \n" +
                        "            \"nomeContato\", comentario,\"qtdPessoas\",cod)\n" +
                        "    VALUES (?, ?, ?, ?, ?, \n" +
                        "            ?,?,?,?);\n")) {
                    reserva.setUuid(UUID.randomUUID());
                    preparedStatement.setObject(1, reserva.getUuid());
                    preparedStatement.setObject(2, reserva.getEstabelecimento().getUuid());
                    if (reserva.getCliente() != null) {
                        preparedStatement.setObject(3, reserva.getCliente().getUuid());
                    } else {
                        preparedStatement.setObject(3, null);
                    }
                    preparedStatement.setTimestamp(4, reserva.getDataReserva());
                    preparedStatement.setString(5, reserva.getTelefoneContato());
                    preparedStatement.setString(6, reserva.getNomeContato());
                    preparedStatement.setString(7, reserva.getComentario());
                    preparedStatement.setInt(8, reserva.getQtdPessoas());
                    preparedStatement.setLong(9, reserva.getCod());
                    preparedStatement.executeUpdate();
                    connection.commit();
                    try {
                        SistemaDelivery sistemaDelivery = ControleSessions.getInstance().getSessionForEstabelecimento(reserva.getEstabelecimento());
                        if (sistemaDelivery.getBroadcaster() != null) {
                            sistemaDelivery.getBroadcaster().broadcast(sistemaDelivery.getSse().newEvent("nova-reserva", reserva.getUuid().toString()));
                        }
                    } catch (IOException e) {
                        throw e;
                    }
                    return true;
                } catch (SQLException | IOException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE public.\"Reservas\"\n" +
                        "   SET \"dataReserva\"=?, \n" +
                        "       \"telefoneContato\"=?, \"nomeContato\"=?, comentario=?, impresso=?, \n" +
                        "       \"qtdPessoas\"=?\n" +
                        " WHERE uuid = ? and uuid_estabelecimento = ?;")) {
                    preparedStatement.setTimestamp(1, reserva.getDataReserva());
                    preparedStatement.setString(2, reserva.getTelefoneContato());
                    preparedStatement.setString(3, reserva.getNomeContato());
                    preparedStatement.setString(4, reserva.getComentario());
                    preparedStatement.setBoolean(5, reserva.isImpresso());
                    preparedStatement.setInt(6, reserva.getQtdPessoas());
                    preparedStatement.setObject(7, reserva.getUuid());
                    preparedStatement.setObject(8, reserva.getEstabelecimento().getUuid());
                    preparedStatement.executeUpdate();
                    connection.commit();
                    if (reservas.containsKey(reserva.getUuid())) {
                        Utilitarios.atualizarObjeto(reservas.get(reserva.getUuid()), reserva);
                    }
                    return true;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        } catch (SQLException | IOException ex) {
            throw ex;
        }
    }

    public boolean excluirReserva(Reserva reserva) throws SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("delete from \"Reservas\" where uuid = ? and uuid_estabelecimento = ?")) {
                preparedStatement.setObject(1, reserva.getUuid());
                preparedStatement.setObject(2, reserva.getEstabelecimento().getUuid());
                if (preparedStatement.executeUpdate() != 1) {
                    throw new SQLException("Falha ao atualizar");
                }
                connection.commit();
                synchronized (reserva) {
                    if (reservas.containsKey(reserva.getUuid())) {
                        reservas.remove(reserva.getUuid());
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

    public List<Reserva> getReservasCliente(Cliente cliente, Estabelecimento estabelecimento) throws SQLException {
        List<Reserva> reservas = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select * from \"Reservas\" where uuid_cliente = ? and uuid_estabelecimento = ?")) {
            preparedStatement.setObject(1, cliente.getUuid());
            preparedStatement.setObject(2, estabelecimento.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    reservas.add(getReservaByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return reservas;
    }

    public List<Reserva> getReservasEstabelecimento(Estabelecimento estabelecimento) throws SQLException {
        List<Reserva> reservas = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select * from \"Reservas\" where uuid_estabelecimento = ?")) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    reservas.add(getReservaByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return reservas;
    }

    public List<Reserva> getReservasImprimir(Estabelecimento estabelecimento) throws SQLException {
        List<Reserva> reservas = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select * from \"Reservas\" where uuid_estabelecimento = ? and impresso = false")) {
            preparedStatement.setObject(1, estabelecimento.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    reservas.add(getReservaByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return reservas;
    }

    public List<Reserva> getReservasCliente(Cliente cliente) throws SQLException {
        List<Reserva> reservas = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select * from \"Reservas\" where uuid_cliente = ?")) {
            preparedStatement.setObject(1, cliente.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    reservas.add(getReservaByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return reservas;
    }

}
