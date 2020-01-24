/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;

/**
 * @author jvbor
 */
public class HandlerSolicitarHorarioReserva extends HandlerBotDelivery {

    public HandlerSolicitarHorarioReserva(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Para que horas seria sua reserva?").join();
        chat.getChat().sendMessage("*Obs*: Envie a hora no seguinte formato *hh:mm*. Ex: *18:45*").join();
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        String dataS = m.getContent().trim().replaceAll(" ", "");
        try {
            LocalDateTime localDateTimeAtual = getChatBotDelivery().getEstabelecimento().getDataComHoraAtual();
            LocalTime horaInformada = LocalTime.parse(dataS, DateTimeFormatter.ofPattern("HH:mm"));
            LocalDateTime localDateTimeReserva = getChatBotDelivery().getReservaAtual().getDataReserva().
                    withSecond(0).
                    withNano(0).
                    withHour(horaInformada.getHour()).
                    withMinute(horaInformada.getMinute());
            if (localDateTimeReserva.toLocalTime().isAfter(getChatBotDelivery().getEstabelecimento().getHoraInicioReservas()) || localDateTimeReserva.getDayOfYear() > localDateTimeAtual.getDayOfYear()) {
                getChatBotDelivery().getReservaAtual().setDataReserva(localDateTimeReserva);
                chat.setHandler(new HandlerSolicitarQuantidadePessoasReserva(chat), true);
                return true;
            } else {
                return false;
            }
        } catch (DateTimeParseException ex) {
            return false;
        } catch (Exception e) {
            getChatBotDelivery().getChat().getDriver().onError(e);
            return false;
        }
    }

    @Override
    protected void onError(Message m) {
        chat.getChat().sendMessage("A hora informada é invalida, tente novamente").join();
        chat.getChat().sendMessage("*Obs¹*: Envie a hora no seguinte formato *hh:mm*. Ex: *18:50*").join();
        chat.getChat().sendMessage("*Obs²*: Os horarios de reservas disponíveis são apenas para após as *" + getChatBotDelivery().getEstabelecimento().getHoraInicioReservas().format(DateTimeFormatter.ofPattern("HH:mm")) + "*").join();
        chat.getChat().sendMessage("*Obs³*: Você não pode solicitar um horario de reserva para um horario anterior à: *" + getChatBotDelivery().getTimeFormat().format(Calendar.getInstance().getTime()) + "*").join();
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
