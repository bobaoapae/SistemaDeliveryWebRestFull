package sistemaDelivery.controle;

import DAO.Conexao;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import restFul.controle.ControleSessions;
import restFul.modelo.Usuario;
import sistemaDelivery.modelo.Estabelecimento;
import utils.Utilitarios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ControleEstabelecimentos {

    private static ControleEstabelecimentos instace;
    private Map<UUID, Estabelecimento> estabelecimentos;

    private ControleEstabelecimentos() {
        this.estabelecimentos = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleEstabelecimentos getInstace() {
        if (instace == null) {
            instace = new ControleEstabelecimentos();
        }
        return instace;
    }

    public Estabelecimento getEstabelecimentoByUUID(UUID uuid) {
        if (estabelecimentos.containsKey(uuid)) {
            return estabelecimentos.get(uuid);
        }
        synchronized (estabelecimentos) {
            try {
                QueryRunner queryRunner = new QueryRunner(Conexao.getDataSource());
                ResultSetHandler<Estabelecimento> h = new BeanHandler<Estabelecimento>(Estabelecimento.class);
                Estabelecimento estabelecimento = queryRunner.query("select * from \"Estabelecimentos\" where uuid = ?", h, uuid);
                if (estabelecimento == null) {
                    return null;
                }
                estabelecimentos.put(uuid, estabelecimento);
                estabelecimento.setCategorias(ControleCategorias.getInstace().getCategoriasEstabelecimento(estabelecimento));
                estabelecimento.setRodizios(ControleRodizios.getInstace().getRodiziosEstabelecimento(estabelecimento));
                return estabelecimento;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public boolean criarEstabelecimento(Usuario usuario, Estabelecimento estabelecimento) {
        if (estabelecimento.getUuid() != null && getEstabelecimentoByUUID(estabelecimento.getUuid()) != null) {
            return this.salvarEstabelecimento(estabelecimento);
        }
        try (Connection connection = Conexao.getConnection();) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO \"Estabelecimentos\"(\n" +
                    "            uuid, \"nomeEstabelecimento\", \"nomeBot\", \"numeroAviso\", \"tempoMedioRetirada\", \n" +
                    "            \"tempoMedioEntrega\", reservas, \n" +
                    "            \"reservasComPedidosFechados\", \"abrirFecharPedidosAutomatico\", \n" +
                    "            \"agendamentoDePedidos\", \"horaAutomaticaAbrirPedidos\", \n" +
                    "            \"horaAutomaticaFecharPedidos\", \"horaInicioReservas\", \"taxaEntregaFixa\", \n" +
                    "            \"taxaEntregaKm\",\"webHookNovaReserva\",\"webHookNovoPedido\", \"logo\",\"valorSelo\",\"maximoSeloPorCompra\",\"validadeSeloFidelidade\")\n" +
                    "    VALUES (?, ?, ?, ?, ?, \n" +
                    "            ?, ?, ?, ?, \n" +
                    "            ?, ?, \n" +
                    "            ?, ?, ?, \n" +
                    "            ?,?,?,?,?,?,?);")) {
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
                preparedStatement.setBoolean(9, estabelecimento.isAbrirFecharPedidosAutomaticamente());
                preparedStatement.setBoolean(10, estabelecimento.isAgendamentoDePedidos());
                if (estabelecimento.isAbrirFecharPedidosAutomaticamente()) {
                    preparedStatement.setTime(11, estabelecimento.getHoraAutomaticaAbrirPedidos());
                    preparedStatement.setTime(12, estabelecimento.getHoraAutomaticaFecharPedidos());
                } else {
                    preparedStatement.setTime(11, null);
                    preparedStatement.setTime(12, null);
                }
                if (estabelecimento.isReservas()) {
                    preparedStatement.setTime(13, estabelecimento.getHoraInicioReservas());
                } else {
                    preparedStatement.setTime(13, null);
                }
                preparedStatement.setDouble(14, estabelecimento.getTaxaEntregaFixa());
                preparedStatement.setDouble(15, estabelecimento.getTaxaEntregaKm());
                preparedStatement.setString(16, estabelecimento.getWebHookNovaReserva());
                preparedStatement.setString(17, estabelecimento.getWebHookNovoPedido());
                preparedStatement.setString(18, estabelecimento.getLogo());
                preparedStatement.setDouble(19, estabelecimento.getValorSelo());
                preparedStatement.setInt(20, estabelecimento.getMaximoSeloPorCompra());
                preparedStatement.setInt(21, estabelecimento.getValidadeSeloFidelidade());
                preparedStatement.executeUpdate();

                try (PreparedStatement preparedStatement2 = connection.prepareStatement("insert into \"Estabelecimentos_Usuario\" (uuid_usuario, uuid_estabelecimento) values (?,?)");) {
                    preparedStatement2.setObject(1, usuario.getUuid());
                    preparedStatement2.setObject(2, estabelecimento.getUuid());
                    preparedStatement2.executeUpdate();
                } catch (SQLException ex) {
                    throw ex;
                }
                connection.commit();
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

    public boolean salvarEstabelecimento(Estabelecimento estabelecimento) {
        try (Connection connection = Conexao.getConnection();) {
            if (estabelecimento.getUuid() != null) {
                connection.setAutoCommit(false);
                try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE \"Estabelecimentos\"\n" +
                        "   SET \"nomeEstabelecimento\"=?, \"nomeBot\"=?, \"numeroAviso\"=?, \n" +
                        "       \"tempoMedioRetirada\"=?, \"tempoMedioEntrega\"=?, \"openPedidos\"=?, \n" +
                        "       \"openChatBot\"=?, reservas=?, \"reservasComPedidosFechados\"=?, \n" +
                        "       \"abrirFecharPedidosAutomatico\"=?, \"agendamentoDePedidos\"=?, \"horaAberturaPedidos\"=?, \n" +
                        "       \"horaAutomaticaAbrirPedidos\"=?, \"horaAutomaticaFecharPedidos\"=?, \n" +
                        "       \"horaInicioReservas\"=?, \"taxaEntregaFixa\"=?, \"taxaEntregaKm\"=?, \n" +
                        "       \"webHookNovaReserva\"=? , \"webHookNovoPedido\"=?, logo=?,  \"valorSelo\"=?, \"maximoSeloPorCompra\"=?, \"validadeSeloFidelidade\"=?\n" +
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
                    preparedStatement.setBoolean(10, estabelecimento.isAbrirFecharPedidosAutomaticamente());
                    preparedStatement.setBoolean(11, estabelecimento.isAgendamentoDePedidos());
                    preparedStatement.setTimestamp(12, new java.sql.Timestamp(estabelecimento.getHoraAberturaPedidos() == null ? new Date().getTime() : estabelecimento.getHoraAberturaPedidos().getTime()));
                    if (estabelecimento.isAbrirFecharPedidosAutomaticamente()) {
                        preparedStatement.setTime(13, estabelecimento.getHoraAutomaticaAbrirPedidos());
                        preparedStatement.setTime(14, estabelecimento.getHoraAutomaticaFecharPedidos());
                    } else {
                        preparedStatement.setTime(13, null);
                        preparedStatement.setTime(14, null);
                    }
                    if (estabelecimento.isReservas()) {
                        preparedStatement.setTime(15, estabelecimento.getHoraInicioReservas());
                    } else {
                        preparedStatement.setTime(15, null);
                    }
                    preparedStatement.setDouble(16, estabelecimento.getTaxaEntregaFixa());
                    preparedStatement.setDouble(17, estabelecimento.getTaxaEntregaKm());
                    preparedStatement.setString(18, estabelecimento.getWebHookNovaReserva());
                    preparedStatement.setString(19, estabelecimento.getWebHookNovoPedido());
                    preparedStatement.setString(20, estabelecimento.getLogo());
                    preparedStatement.setDouble(21, estabelecimento.getValorSelo());
                    preparedStatement.setInt(22, estabelecimento.getMaximoSeloPorCompra());
                    preparedStatement.setInt(23, estabelecimento.getValidadeSeloFidelidade());
                    preparedStatement.setObject(24, estabelecimento.getUuid());
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean excluirEstabelecimento(Estabelecimento estabelecimento) {
        try (Connection connection = Conexao.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("update \"Estabelecimentos\" set ativo = ? where uuid = ?")) {
                preparedStatement.setBoolean(1, false);
                preparedStatement.setObject(2, estabelecimento.getUuid());
                preparedStatement.executeUpdate();
                connection.commit();
                if (ControleSessions.getInstance().checkSessionAtiva(estabelecimento)) {
                    ControleSessions.getInstance().finalizarSessionForEstabelecimento(estabelecimento);
                }
                synchronized (estabelecimentos) {
                    estabelecimentos.remove(estabelecimento.getUuid());
                }
                return true;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Estabelecimento> getEstabelecimentosUsuario(Usuario u) {
        List<Estabelecimento> estabelecimentos = new ArrayList<>();
        try (Connection conn = Conexao.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement("select uuid_estabelecimento from \"Estabelecimentos_Usuario\" as a inner join \"Estabelecimentos\" as b on a.uuid_estabelecimento=b.uuid where uuid_usuario = ? and ativo");
        ) {
            preparedStatement.setObject(1, u.getUuid());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    estabelecimentos.add(getEstabelecimentoByUUID(UUID.fromString(resultSet.getString("uuid_estabelecimento"))));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return estabelecimentos;
    }

}
