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
public class HandlerSolicitaNomeCadastro extends HandlerBotDelivery {

    public HandlerSolicitaNomeCadastro(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Beleza, me informe seu nome completo!").join();
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        if (m.getContent().trim().length() > 0) {
            getChatBotDelivery().getCliente().setNome(m.getContent().trim());
            chat.setHandler(new HandlerVerificaNumeroCadastro(chat), true);
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
