/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.ChatBotDelivery;

/**
 * @author jvbor
 */
public class HandlerVerificaNomeContatoReserva extends HandlerBotDelivery {

    public HandlerVerificaNomeContatoReserva(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Agora preciso do nome para contato, já tenho o seguinte nome anotado aqui: " + ((ChatBotDelivery) chat).getNome());
        chat.getChat().sendMessage("O nome está correto?");
        chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*");
        chat.getChat().sendMessage("1 - Sim");
        chat.getChat().sendMessage("2 - Não");
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        if (m.getContent().trim().equals("1")) {
            ((ChatBotDelivery) chat).getReservaAtual().setNomeContato(((ChatBotDelivery) chat).getNome());
            chat.setHandler(new HandlerVerificaNumeroContatoReserva(chat), true);
            return true;
        } else if (m.getContent().trim().equals("2")) {
            chat.setHandler(new HandlerSolicitaNomeContatoReserva(chat), true);
            return true;
        }
        return false;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
