package sistemaDelivery.controle;

import DAO.Conexao;
import modelo.EstadoDriver;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import restFul.controle.ControleSessions;
import restFul.modelo.TipoUsuario;
import restFul.modelo.Usuario;
import sistemaDelivery.modelo.Estabelecimento;
import sistemaDelivery.modelo.TipoEntrega;
import utils.Utilitarios;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.util.*;

public class ControleEstabelecimentos {

    private static final Object syncronizeGetInstance = new Object();
    private static ControleEstabelecimentos instance;
    private Map<UUID, Estabelecimento> estabelecimentos;

    private ControleEstabelecimentos() {
        this.estabelecimentos = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleEstabelecimentos getInstance() {
        synchronized (syncronizeGetInstance) {
            if (instance == null) {
                instance = new ControleEstabelecimentos();
            }
            return instance;
        }
    }

    public Estabelecimento getEstabelecimentoByUUID(UUID uuid) throws SQLException {
        if (estabelecimentos.containsKey(uuid)) {
            return estabelecimentos.get(uuid);
        }
        synchronized (estabelecimentos) {
            QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
            ResultSetHandler<Estabelecimento> h = new BeanHandler<>(Estabelecimento.class);
            Estabelecimento estabelecimento = queryRunner.query("select * from \"Estabelecimentos\" where uuid = ?", h, uuid);
            if (estabelecimento == null) {
                return null;
            }
            estabelecimentos.putIfAbsent(uuid, estabelecimento);
            estabelecimento.setCategorias(ControleCategorias.getInstance().getCategoriasEstabelecimento(estabelecimento));
            estabelecimento.setRodizios(ControleRodizios.getInstace().getRodiziosEstabelecimento(estabelecimento));
            estabelecimento.setTiposEntregas(ControleTiposEntrega.getInstance().getTiposEntregasEstabelecimento(estabelecimento));
            estabelecimento.setHorariosFuncionamento(ControleHorariosFuncionamento.getInstance().getHorariosFuncionamento(estabelecimento));
            return estabelecimentos.get(uuid);
        }
    }

    public boolean criarEstabelecimento(Usuario usuario, Estabelecimento estabelecimento) throws SQLException {
        if (estabelecimento.getUuid() != null && getEstabelecimentoByUUID(estabelecimento.getUuid()) != null) {
            return this.salvarEstabelecimento(estabelecimento);
        }
        try (Connection connection = Conexao.getConnection();) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO \"Estabelecimentos\"(\n" +
                    "            uuid, \"nomeEstabelecimento\", \"nomeBot\", \"numeroAviso\", \"tempoMedioRetirada\", \n" +
                    "            \"tempoMedioEntrega\", reservas, \n" +
                    "            \"reservasComPedidosFechados\", \"abrirFecharPedidosAutomatico\", \n" +
                    "            \"agendamentoDePedidos\", \n" +
                    "            \"horaInicioReservas\", \n" +
                    "            \"webHookNovaReserva\",\"webHookNovoPedido\", " +
                    "            \"logo\",\"valorSelo\",\"maximoSeloPorCompra\"," +
                    "            \"validadeSeloFidelidade\",\"timeZone\",endereco)\n" +
                    "    VALUES (?, ?, ?, ?, ?, \n" +
                    "            ?, ?, ?, ?, \n" +
                    "            ?, ?, \n" +
                    "            ?, ?, ?, \n" +
                    "            ?, ?, ?, ?, ?);")) {
                if (estabelecimento.getUuid() == null) {
                    estabelecimento.setUuid(UUID.randomUUID());
                }
                preparedStatement.setObject(1, estabelecimento.getUuid());
                preparedStatement.setString(2, estabelecimento.getNomeEstabelecimento());
                preparedStatement.setString(3, estabelecimento.getNomeBot());
                preparedStatement.setString(4, estabelecimento.getNumeroAviso());
                preparedStatement.setInt(5, estabelecimento.getTempoMedioRetirada());
                preparedStatement.setInt(6, estabelecimento.getTempoMedioEntrega());
                preparedStatement.setBoolean(7, estabelecimento.isReservas());
                preparedStatement.setBoolean(8, estabelecimento.isReservasComPedidosFechados());
                preparedStatement.setBoolean(9, estabelecimento.isAbrirFecharPedidosAutomatico());
                preparedStatement.setBoolean(10, estabelecimento.isAgendamentoDePedidos());
                preparedStatement.setObject(11, estabelecimento.getHoraInicioReservas());
                preparedStatement.setString(12, estabelecimento.getWebHookNovaReserva());
                preparedStatement.setString(13, estabelecimento.getWebHookNovoPedido());
                preparedStatement.setString(14, estabelecimento.getLogo());
                preparedStatement.setDouble(15, estabelecimento.getValorSelo());
                preparedStatement.setInt(16, estabelecimento.getMaximoSeloPorCompra());
                preparedStatement.setInt(17, estabelecimento.getValidadeSeloFidelidade());
                preparedStatement.setString(18, estabelecimento.getTimeZoneObject().toZoneId().getDisplayName(TextStyle.NARROW, Locale.forLanguageTag("pt-BR")));
                preparedStatement.setString(19, estabelecimento.getEndereco());
                preparedStatement.executeUpdate();

                try (PreparedStatement preparedStatement2 = connection.prepareStatement("insert into \"Estabelecimentos_Usuario\" (uuid_usuario, uuid_estabelecimento) values (?,?)");) {
                    preparedStatement2.setObject(1, usuario.getUuid());
                    preparedStatement2.setObject(2, estabelecimento.getUuid());
                    preparedStatement2.executeUpdate();
                }
                for (TipoEntrega tipoEntrega : estabelecimento.getTiposEntregas()) {
                    tipoEntrega.setEstabelecimento(estabelecimento);
                    ControleTiposEntrega.getInstance().salvarTipoEntrega(tipoEntrega, connection);
                }
                connection.commit();
                return true;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean salvarEstabelecimento(Estabelecimento estabelecimento) throws SQLException {
        try (Connection connection = Conexao.getConnection();) {
            if (estabelecimento.getUuid() != null) {
                connection.setAutoCommit(false);
                try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE \"Estabelecimentos\"\n" +
                        "   SET \"nomeEstabelecimento\"=?, \"nomeBot\"=?, \"numeroAviso\"=?, \n" +
                        "       \"tempoMedioRetirada\"=?, \"tempoMedioEntrega\"=?, \"openPedidos\"=?, \n" +
                        "       \"openChatBot\"=?, reservas=?, \"reservasComPedidosFechados\"=?, \n" +
                        "       \"abrirFecharPedidosAutomatico\"=?, \"agendamentoDePedidos\"=?, \"horaAberturaPedidos\"=?, \n" +
                        "       \"horaInicioReservas\"=?, \n" +
                        "       \"webHookNovaReserva\"=? , \"webHookNovoPedido\"=?, logo=?,  \"valorSelo\"=?, \"maximoSeloPorCompra\"=?," +
                        " \"validadeSeloFidelidade\"=?, \"timeZone\" = ?, \"iniciarAutomaticamente\" = ?, endereco = ? \n" +
                        " WHERE uuid=?;")) {
                    preparedStatement.setString(1, estabelecimento.getNomeEstabelecimento());
                    preparedStatement.setString(2, estabelecimento.getNomeBot());
                    preparedStatement.setString(3, estabelecimento.getNumeroAviso());
                    preparedStatement.setInt(4, estabelecimento.getTempoMedioRetirada());
                    preparedStatement.setInt(5, estabelecimento.getTempoMedioEntrega());
                    preparedStatement.setBoolean(6, estabelecimento.isOpenPedidos());
                    preparedStatement.setBoolean(7, estabelecimento.isOpenChatBot());
                    preparedStatement.setBoolean(8, estabelecimento.isReservas());
                    preparedStatement.setBoolean(9, estabelecimento.isReservasComPedidosFechados());
                    preparedStatement.setBoolean(10, estabelecimento.isAbrirFecharPedidosAutomatico());
                    preparedStatement.setBoolean(11, estabelecimento.isAgendamentoDePedidos());
                    preparedStatement.setObject(12, estabelecimento.getHoraAberturaPedidos() == null ? OffsetDateTime.now() : estabelecimento.getHoraAberturaPedidos());
                    preparedStatement.setObject(13, estabelecimento.getHoraInicioReservas());
                    preparedStatement.setString(14, estabelecimento.getWebHookNovaReserva());
                    preparedStatement.setString(15, estabelecimento.getWebHookNovoPedido());
                    preparedStatement.setString(16, estabelecimento.getLogo());
                    preparedStatement.setDouble(17, estabelecimento.getValorSelo());
                    preparedStatement.setInt(18, estabelecimento.getMaximoSeloPorCompra());
                    preparedStatement.setInt(19, estabelecimento.getValidadeSeloFidelidade());
                    preparedStatement.setString(20, estabelecimento.getTimeZoneObject().toZoneId().getDisplayName(TextStyle.NARROW, Locale.forLanguageTag("pt-BR")));
                    preparedStatement.setBoolean(21, estabelecimento.isIniciarAutomaticamente());
                    preparedStatement.setString(22, estabelecimento.getEndereco());
                    preparedStatement.setObject(23, estabelecimento.getUuid());
                    preparedStatement.executeUpdate();
                    preparedStatement.close();
                    connection.commit();
                    if (estabelecimentos.containsKey(estabelecimento.getUuid())) {
                        Utilitarios.atualizarObjeto(estabelecimentos.get(estabelecimento.getUuid()), estabelecimento);
                    }
                    return true;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            } else {
                return false;
            }
        }
    }

    public boolean excluirEstabelecimento(Estabelecimento estabelecimento) throws IOException, SQLException {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Estabelecimentos\" set ativo = ? where uuid = ?")) {
                preparedStatement.setBoolean(1, false);
                preparedStatement.setObject(2, estabelecimento.getUuid());
                preparedStatement.executeUpdate();
                connection.commit();
                if (ControleSessions.getInstance().checkSessionAtiva(estabelecimento)) {
                    var session = ControleSessions.getInstance().getSessionForEstabelecimento(estabelecimento);
                    if (session.getDriver().getEstadoDriver() == EstadoDriver.LOGGED) {
                        ControleSessions.getInstance().getSessionForEstabelecimento(estabelecimento).logout();
                    }
                    new Thread(() -> {
                        try {
                            Thread.sleep(2500);
                            ControleSessions.getInstance().finalizarSessionForEstabelecimento(estabelecimento);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
                synchronized (estabelecimentos) {
                    estabelecimentos.remove(estabelecimento.getUuid());
                }
                return true;
            } catch (SQLException | IOException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<Estabelecimento> getEstabelecimentosIniciarAutomaticamente() throws SQLException {
        List<Estabelecimento> estabelecimentos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid from \"Estabelecimentos\" where \"iniciarAutomaticamente\" and ativo");
        ) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    estabelecimentos.add(getEstabelecimentoByUUID(UUID.fromString(resultSet.getString("uuid"))));
                }
            }
        }
        return estabelecimentos;
    }

    public List<Estabelecimento> getEstabelecimentosUsuario(Usuario u) throws SQLException {
        List<Estabelecimento> estabelecimentos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid_estabelecimento from \"Estabelecimentos_Usuario\" as a " +
                     "inner join \"Estabelecimentos\" as b on a.uuid_estabelecimento=b.uuid where uuid_usuario = ? and ativo or ? order by b.\"dataCriacao\"");
        ) {
            preparedStatement.setObject(1, u.getUuid());
            preparedStatement.setBoolean(2, u.getTipoUsuario() == TipoUsuario.SUPER_ADMIN);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    estabelecimentos.add(getEstabelecimentoByUUID(UUID.fromString(resultSet.getString("uuid_estabelecimento"))));
                }
            }
        }
        return estabelecimentos;
    }

}
