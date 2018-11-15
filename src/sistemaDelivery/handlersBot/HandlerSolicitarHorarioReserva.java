/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.ChatBotDelivery;
import utils.DateUtils;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
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
        chat.getChat().sendMessage("Para que horas seria sua reserva?");
        chat.getChat().sendMessage("*Obs*: Envie a hora no seguinte formato *hh:mm*. Ex: *18:45*");
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        String dataS = m.getContent().trim().replaceAll(" ", "");
        try {
            Calendar horaAtual = Calendar.getInstance();
            Calendar dataInformada = Calendar.getInstance();
            Calendar dataChat = Calendar.getInstance();
            dataChat.setTimeInMillis(((ChatBotDelivery) chat).getReservaAtual().getDataReserva().getTime());
            dataInformada.setTime(((ChatBotDelivery) chat).getTimeFormat().parse(dataS));
            dataChat.set(Calendar.HOUR_OF_DAY, dataInformada.get(Calendar.HOUR_OF_DAY));
            dataChat.set(Calendar.MINUTE, dataInformada.get(Calendar.MINUTE));
            dataChat.set(Calendar.SECOND, 0);
            dataChat.set(Calendar.MILLISECOND, 0);
            if (((dataInformada.get(Calendar.HOUR_OF_DAY) == getChatBotDelivery().getEstabelecimento().getHoraInicioReservas().toLocalTime().getHour() && dataInformada.get(Calendar.MINUTE) >= getChatBotDelivery().getEstabelecimento().getHoraInicioReservas().toLocalTime().getMinute()) || dataInformada.get(Calendar.HOUR_OF_DAY) > getChatBotDelivery().getEstabelecimento().getHoraInicioReservas().toLocalTime().getHour()) && ((dataChat.get(Calendar.HOUR_OF_DAY) == horaAtual.get(Calendar.HOUR_OF_DAY) && dataChat.get(Calendar.MINUTE) > horaAtual.get(Calendar.MINUTE)) || dataChat.get(Calendar.HOUR_OF_DAY) > horaAtual.get(Calendar.HOUR_OF_DAY) || DateUtils.isAfterDay(((ChatBotDelivery) chat).getReservaAtual().getDataReserva(), Calendar.getInstance().getTime()))) {
                ((ChatBotDelivery) chat).getReservaAtual().setDataReserva(new Timestamp(dataChat.getTime().getTime()));
                chat.setHandler(new HandlerSolicitarQuantidadePessoasReserva(chat), true);
                return true;
            } else {
                return false;
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onError(Message m) {
        chat.getChat().sendMessage("A hora informada é invalida, tente novamente");
        chat.getChat().sendMessage("*Obs¹*: Envie a hora no seguinte formato *hh:mm*. Ex: *18:50*");
        chat.getChat().sendMessage("*Obs²*: Os horarios de reservas disponíveis são apenas para após as *" + getChatBotDelivery().getEstabelecimento().getHoraInicioReservas().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + "*");
        chat.getChat().sendMessage("*Obs³*: Você não pode solicitar um horario de reserva para um horario anterior à: *" + ((ChatBotDelivery) chat).getTimeFormat().format(Calendar.getInstance().getTime()) + "*");
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
