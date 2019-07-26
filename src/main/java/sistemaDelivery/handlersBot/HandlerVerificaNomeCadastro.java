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
        chat.getChat().sendMessage("O seu nome está correto?");
        chat.getChat().sendMessage("*" + getChatBotDelivery().getNome() + "*");
        chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*");
        chat.getChat().sendMessage("*1* - Sim");
        chat.getChat().sendMessage("*2* - Não");
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
