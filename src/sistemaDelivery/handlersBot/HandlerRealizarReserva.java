/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.ChatBotDelivery;
import sistemaDelivery.modelo.Reserva;
import utils.DateUtils;

import java.text.ParseException;
import java.util.Calendar;

/**
 * @author jvbor
 */
public class HandlerRealizarReserva extends HandlerBotDelivery {

    public HandlerRealizarReserva(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Certo, para poder realizar a sua reserva preciso anotar alguns dados");
        chat.getChat().sendMessage("A primeira informação que preciso é a data da reserva.");
        chat.getChat().sendMessage("*Obs*: Envie a data no seguinte formato *dd/mm*. Ex: *" + ((ChatBotDelivery) chat).getDateFormat().format(Calendar.getInstance().getTime()) + "*");
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        String dataS = m.getContent().trim().replaceAll(" ", "");
        try {
            Calendar data = Calendar.getInstance();
            data.setTime(((ChatBotDelivery) chat).getDateFormat().parse(dataS));
            data.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
            if (DateUtils.isToday(data.getTime()) || DateUtils.isAfterDay(data.getTime(), Calendar.getInstance().getTime())) {
                Reserva r = new Reserva();
                r.setDataReserva(data.getTime());
                ((ChatBotDelivery) chat).setReservaAtual(r);
                chat.setHandler(new HandlerSolicitarHorarioReserva(chat), true);
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
        chat.getChat().sendMessage("A data informada é invalida, tente novamente");
        chat.getChat().sendMessage("*Obs*: Envie a data no seguinte formato *dd/mm*. Ex: *" + ((ChatBotDelivery) chat).getDateFormat().format(Calendar.getInstance().getTime()) + "*");
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
