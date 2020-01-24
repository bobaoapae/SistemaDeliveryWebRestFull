/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;

import java.sql.SQLException;

/**
 * @author jvbor
 */
public class HandlerSolicitarFormaPagamento extends HandlerBotDelivery {

    public HandlerSolicitarFormaPagamento(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Qual vai ser a forma do pagamento?").join();
        chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*").join();
        chat.getChat().sendMessage("*1* - 💵 Dinheiro").join();
        chat.getChat().sendMessage("*2* - 💳 Cartão de Crédito").join();
        chat.getChat().sendMessage("*3* - 💳💵 Dinheiro e Cartão de Crédito").join();
        try {
            if (getChatBotDelivery().getCliente().getCreditosDisponiveis() > 0) {
                chat.getChat().sendMessage("*4* - Créditos de Recarga").join();
            }
        } catch (SQLException e) {
            getChatBotDelivery().getChat().getDriver().onError(e);
        }
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        if (msg.getContent().trim().equals("1")) {
            getChatBotDelivery().getPedidoAtual().setCartao(false);
            chat.setHandler(new HandlerSolicitarTroco(chat), true);
        } else if (msg.getContent().trim().equals("2")) {
            getChatBotDelivery().getPedidoAtual().setCartao(true);
            chat.setHandler(new HandlerDesejaAgendar(chat), true);
        } else if (msg.getContent().trim().equals("3")) {
            getChatBotDelivery().getPedidoAtual().setCartao(true);
            getChatBotDelivery().getPedidoAtual().setTroco(-1);
            chat.setHandler(new HandlerSolicitarTroco(chat), true);
        } else {
            try {
                if (msg.getContent().trim().equals("4") && getChatBotDelivery().getCliente().getCreditosDisponiveis() > 0) {
                    chat.setHandler(new HandlerPagarComCreditos(chat), true);
                } else {
                    return false;
                }
            } catch (SQLException e) {
                getChatBotDelivery().getChat().getDriver().onError(e);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
