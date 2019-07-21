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
public class HandlerChatExpirado extends HandlerBotDelivery {

    public HandlerChatExpirado(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage(getChatBotDelivery().getNome() + ", você ficou ausente por muito tempo, vamos ter que começar novamente");
        chat.setHandler(new HandlerBoasVindas(chat), true);
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        return true;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
