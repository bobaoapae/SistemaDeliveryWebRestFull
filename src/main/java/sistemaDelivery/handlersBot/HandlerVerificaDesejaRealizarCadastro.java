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
public class HandlerVerificaDesejaRealizarCadastro extends HandlerBotDelivery {

    public HandlerVerificaDesejaRealizarCadastro(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Vejo aqui que você não concluiu seu cadastro ainda, com o cadastro você pode ganhar *selos de fidelidade*, participa de *promoções*, ganha *descontos exclusivos* e possui muitas outras *vantagens*!");
        chat.setHandler(new HandlerVerificaNomeCadastro(chat), true);
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        return runFirstTime(msg);
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
