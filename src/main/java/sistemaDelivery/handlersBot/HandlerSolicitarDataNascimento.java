/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;

/**
 * @author jvbor
 */
public class HandlerSolicitarDataNascimento extends HandlerBotDelivery {

    SimpleDateFormat formatador = new SimpleDateFormat("dd/MM/yyyy");

    public HandlerSolicitarDataNascimento(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Certo, para poder finalizar seu cadastro só preciso de mais uma informação");
        chat.getChat().sendMessage("Me informe sua data de nascimento.");
        chat.getChat().sendMessage("*Obs*: Envie a data no seguinte formato *dd/mm/aaaa*. Ex: *" + formatador.format(Calendar.getInstance().getTime()) + "*");
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        String dataS = m.getContent().trim().replaceAll(" ", "");
        try {
            getChatBotDelivery().getCliente().setDataAniversario(Date.valueOf(LocalDate.parse(dataS, DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
            chat.setHandler(new HandlerFinalizarCadastro(chat), true);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        } catch (Exception ex) {
            getChatBotDelivery().getChat().getDriver().onError(ex);
            return false;
        }
    }

    @Override
    protected void onError(Message m) {
        chat.getChat().sendMessage("A data informada é invalida, tente novamente");
        chat.getChat().sendMessage("*Obs*: Envie a data no seguinte formato *dd/mm/aaaa*. Ex: *" + formatador.format(Calendar.getInstance().getTime()) + "*");
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
