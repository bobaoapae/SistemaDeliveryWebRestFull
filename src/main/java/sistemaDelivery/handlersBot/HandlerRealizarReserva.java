/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.Reserva;
import utils.Utils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * @author jvbor
 */
public class HandlerRealizarReserva extends HandlerBotDelivery {

    public HandlerRealizarReserva(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Certo, para poder realizar a sua reserva preciso anotar alguns dados").join();
        chat.getChat().sendMessage("A primeira informação que preciso é a data da reserva.").join();
        chat.getChat().sendMessage("*Obs*: Envie a data no seguinte formato *dd/mm*. Ex: *" + getChatBotDelivery().getEstabelecimento().getDataComHoraAtual().format(DateTimeFormatter.ofPattern("dd/MM")) + "*").join();
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        String dataS = m.getContent().trim().replaceAll(" ", "");
        try {
            LocalDateTime localDateTimeAtual = getChatBotDelivery().getEstabelecimento().getDataComHoraAtual();
            MonthDay monthDay = Utils.tryParseDataSemAno(dataS);
            LocalDateTime localDateTime = LocalDateTime.of(monthDay.atYear(localDateTimeAtual.getYear()), LocalTime.MIDNIGHT);
            if (localDateTime.toLocalDate().equals(localDateTimeAtual.toLocalDate()) || localDateTime.toLocalDate().isAfter(localDateTimeAtual.toLocalDate())) {
                Reserva r = new Reserva();
                r.setCliente(getChatBotDelivery().getCliente());
                r.setEstabelecimento(getChatBotDelivery().getEstabelecimento());
                r.setDataReserva(localDateTime);
                getChatBotDelivery().setReservaAtual(r);
                chat.setHandler(new HandlerSolicitarHorarioReserva(chat), true);
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
        chat.getChat().sendMessage("A data informada é invalida, tente novamente").join();
        chat.getChat().sendMessage("*Obs*: Envie a data no seguinte formato *dd/mm*. Ex: *" + getChatBotDelivery().getEstabelecimento().getDataComHoraAtual().format(DateTimeFormatter.ofPattern("dd/MM")) + "*").join();
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
