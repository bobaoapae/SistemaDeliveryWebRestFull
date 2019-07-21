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
public class HandlerSolicitaNumeroCadastro extends HandlerBotDelivery {

    public HandlerSolicitaNumeroCadastro(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Beleza, me envie o número para contato!");
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        if (m.getContent().trim().length() >= 8) {
            getChatBotDelivery().getCliente().setTelefoneMovel(m.getContent().trim());
            chat.setHandler(new HandlerSolicitarDataNascimento(chat), true);
            return true;
        }
        return false;
    }

    @Override
    protected void onError(Message m) {
        chat.getChat().sendMessage("O número informado é invalido, por favor informe seu número novamente");
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
