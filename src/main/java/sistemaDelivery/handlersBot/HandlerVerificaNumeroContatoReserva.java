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
public class HandlerVerificaNumeroContatoReserva extends HandlerBotDelivery {

    public HandlerVerificaNumeroContatoReserva(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Seu número para contato é esse mesmo que estamos conversando? \n" + ((ChatBotDelivery) chat).getCliente().getTelefoneMovel());
        chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*");
        chat.getChat().sendMessage("1 - Sim");
        chat.getChat().sendMessage("2 - Não");
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        if (m.getContent().trim().equals("1")) {
            ((ChatBotDelivery) chat).getReservaAtual().setTelefoneContato(((ChatBotDelivery) chat).getCliente().getTelefoneMovel());
            chat.setHandler(new HandlerFinalizarReserva(chat), true);
            return true;
        } else if (m.getContent().trim().equals("2")) {
            chat.setHandler(new HandlerSolicitaNumeroContatoReserva(chat), true);
            return true;
        }
        return false;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
