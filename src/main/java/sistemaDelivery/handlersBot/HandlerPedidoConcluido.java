/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.ChatBotDelivery;
import sistemaDelivery.modelo.Pedido;

/**
 * @author jvbor
 */
public class HandlerPedidoConcluido extends HandlerBotDelivery {

    public HandlerPedidoConcluido(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Existe algo mais em que eu possa lhe ajudar?");
        chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*");
        chat.getChat().sendMessage("1 - Sim");
        chat.getChat().sendMessage("2 - Não");
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        if (msg.getContent().trim().equals("1") || msg.getContent().toLowerCase().trim().equals("sim") || msg.getContent().toLowerCase().trim().equals("s")) {
            ((ChatBotDelivery) chat).setPedidoAtual(new Pedido(((ChatBotDelivery) chat).getCliente(), getChatBotDelivery().getEstabelecimento()));
            chat.setHandler(new HandlerMenuPrincipal(chat), true);
        } else if (msg.getContent().trim().equals("2") || msg.getContent().toLowerCase().trim().equals("não") || msg.getContent().toLowerCase().trim().equals("nao") || msg.getContent().toLowerCase().trim().equals("n")) {
            if (((ChatBotDelivery) chat).getCliente().isCadastroRealizado()) {
                chat.setHandler(new HandlerAdeus(chat), true);
            } else {
                chat.setHandler(new HandlerVerificaDesejaRealizarCadastro(chat), true);
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
