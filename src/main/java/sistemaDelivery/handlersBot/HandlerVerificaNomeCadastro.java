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
public class HandlerVerificaNomeCadastro extends HandlerBotDelivery {

    public HandlerVerificaNomeCadastro(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("O seu nome está correto?").join();
        chat.getChat().sendMessage("*" + getChatBotDelivery().getNome() + "*").join();
        chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*").join();
        chat.getChat().sendMessage("*1* - Sim").join();
        chat.getChat().sendMessage("*2* - Não").join();
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        if (m.getContent().trim().equals("1")) {
            getChatBotDelivery().getCliente().setNome(getChatBotDelivery().getNome());
            chat.setHandler(new HandlerVerificaNumeroCadastro(chat), true);
            return true;
        } else if (m.getContent().trim().equals("2")) {
            chat.setHandler(new HandlerSolicitaNomeCadastro(chat), true);
            return true;
        }
        return false;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
