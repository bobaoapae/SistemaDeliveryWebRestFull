/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * @author jvbor
 */
public class HandlerAgendamentoPedido extends HandlerBotDelivery {

    private LocalTime horaInformada;
    private boolean formatoIncorreto;

    public HandlerAgendamentoPedido(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().markComposing(1000);
        if (getChatBotDelivery().getPedidoAtual().isEntrega()) {
            chat.getChat().sendMessage("Para que horas você gostaria que a entrega fosse feita?");
        } else {
            chat.getChat().sendMessage("Que horas você quer vir buscar o seu pedido?");
        }
        chat.getChat().sendMessage(gerarObs("Envie a hora no seguinte formato *hh:mm*. Ex: *18:45*"));
        resetarObs();
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        String dataS = m.getContent().trim().replaceAll(" ", "");
        try {
            formatoIncorreto = true;
            LocalTime horaAtual = getChatBotDelivery().getEstabelecimento().getHoraAtual();
            horaInformada = LocalTime.parse(dataS, DateTimeFormatter.ofPattern("HH:mm"));
            if ((horaInformada.isAfter(horaAtual) || horaInformada.equals(horaAtual)) && (!getChatBotDelivery().getEstabelecimento().isAbrirFecharPedidosAutomatico() || (getChatBotDelivery().getEstabelecimento().isTimeBeetwenHorarioFuncionamento(horaInformada, getChatBotDelivery().getEstabelecimento().getDataComHoraAtual().getDayOfWeek())))) {
                getChatBotDelivery().getPedidoAtual().setHoraAgendamento(java.sql.Time.valueOf(horaInformada));
                chat.setHandler(new HandlerConcluirPedido(chat), true);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            if (e instanceof DateTimeParseException) {
                formatoIncorreto = true;
            }
            getChatBotDelivery().getChat().getDriver().onError(e);
            return false;
        } finally {
            resetarObs();
        }
    }

    @Override
    protected void onError(Message m) {
        chat.getChat().sendMessage("A hora informada é invalida, tente novamente");
        if (formatoIncorreto) {
            chat.getChat().sendMessage(gerarObs("Envie a hora no seguinte formato *hh:mm*. Ex: *18:50*"));
        }
        if (getChatBotDelivery().getEstabelecimento().nextOrCurrentHorarioAbertoOfDay() != null) {
            chat.getChat().sendMessage(gerarObs("Os horarios de agendamento disponíveis são entre *" + getChatBotDelivery().getEstabelecimento().nextOrCurrentHorarioAbertoOfDay().getHoraAbrir().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " e " + getChatBotDelivery().getEstabelecimento().nextOrCurrentHorarioAbertoOfDay().getHoraFechar().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + "*"));
        }
        if (horaInformada.isBefore(getChatBotDelivery().getEstabelecimento().getHoraAtual())) {
            chat.getChat().sendMessage(gerarObs("Você não pode informar um horario anterior à hora atual: *" + getChatBotDelivery().getEstabelecimento().getHoraAtual().format(DateTimeFormatter.ofPattern("HH:mm")) + "*"));
        }
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
