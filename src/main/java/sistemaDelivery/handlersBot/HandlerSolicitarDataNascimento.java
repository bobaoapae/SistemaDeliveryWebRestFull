/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import utils.Utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author jvbor
 */
public class HandlerSolicitarDataNascimento extends HandlerBotDelivery {

    public HandlerSolicitarDataNascimento(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Certo, para poder finalizar seu cadastro só preciso de mais uma informação");
        chat.getChat().sendMessage("Me informe sua data de nascimento.");
        chat.getChat().sendMessage("*Obs*: Envie a data no seguinte formato *dd/mm/aaaa*. Ex: *" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "*");
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        String dataS = m.getContent().trim().replaceAll(" ", "");
        Date date = Utils.tryParseData(dataS);
        if (date != null) {
            getChatBotDelivery().getCliente().setDataAniversario(new java.sql.Date(date.getTime()));
            chat.setHandler(new HandlerFinalizarCadastro(chat), true);
            return true;
        }
        return false;
    }

    @Override
    protected void onError(Message m) {
        chat.getChat().sendMessage("A data informada é invalida, tente novamente");
        chat.getChat().sendMessage("*Obs*: Envie a data no seguinte formato *dd/mm/aaaa*. Ex: *" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "*");
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
