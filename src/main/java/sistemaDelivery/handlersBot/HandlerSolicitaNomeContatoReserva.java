/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;

/**
 * @author jvbor
 */
public class HandlerSolicitaNomeContatoReserva extends HandlerBotDelivery {

    public HandlerSolicitaNomeContatoReserva(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Beleza, me envie o nome para contato!").join();
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        if (m.getContent().trim().length() > 0) {
            getChatBotDelivery().getReservaAtual().setNomeContato(m.getContent().trim());
            chat.setHandler(new HandlerVerificaNumeroContatoReserva(chat), true);
            return true;
        }
        return false;
    }

    @Override
    protected void onError(Message m) {
        chat.getChat().sendMessage("O nome informado é invalido, por favor informe seu nome novamente").join();
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
