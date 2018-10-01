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
public class HandlerSolicitarFormaPagamento extends HandlerBotDelivery {

    public HandlerSolicitarFormaPagamento(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Qual vai ser a forma do pagamento?");
        chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*");
        chat.getChat().sendMessage("*1* - 💵 Dinheiro");
        chat.getChat().sendMessage("*2* - 💳 Cartão de Crédito");
        chat.getChat().sendMessage("*3* - 💳💵 Dinheiro e Cartão de Crédito");
        if (((ChatBotDelivery) chat).getCliente().getCreditosDisponiveis(getChatBotDelivery().getEstabelecimento()) > 0) {
            chat.getChat().sendMessage("*4* - Créditos de Recarga");
        }
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        if (msg.getContent().trim().equals("1")) {
            ((ChatBotDelivery) chat).getPedidoAtual().setCartao(false);
            chat.setHandler(new HandlerSolicitarTroco(chat), true);
        } else if (msg.getContent().trim().equals("2")) {
            ((ChatBotDelivery) chat).getPedidoAtual().setCartao(true);
            chat.setHandler(new HandlerDesejaAgendar(chat), true);
        } else if (msg.getContent().trim().equals("3")) {
            ((ChatBotDelivery) chat).getPedidoAtual().setCartao(true);
            ((ChatBotDelivery) chat).getPedidoAtual().setTroco(-1);
            chat.setHandler(new HandlerSolicitarTroco(chat), true);
        } else if (msg.getContent().trim().equals("4") && ((ChatBotDelivery) chat).getCliente().getCreditosDisponiveis(getChatBotDelivery().getEstabelecimento()) > 0) {
            chat.setHandler(new HandlerPagarComCreditos(chat), true);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
