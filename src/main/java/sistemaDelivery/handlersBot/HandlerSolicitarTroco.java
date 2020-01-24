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
public class HandlerSolicitarTroco extends HandlerBotDelivery {

    public HandlerSolicitarTroco(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Seu pedido ficou no valor de R$" + moneyFormat.format(getChatBotDelivery().getPedidoAtual().getTotal())).join();
        if (getChatBotDelivery().getPedidoAtual().isCartao()) {
            chat.getChat().sendMessage("Me informe como sera a divisão para que eu possa levar o troco.").join();
        } else {
            chat.getChat().sendMessage("Você vai precisar que levemos troco? Caso precise, basta me informar para quantos reais.").join();
        }
        chat.getChat().sendMessage("*_Obs: Caso não queira troco, basta enviar NÃO_*").join();
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        if (!getChatBotDelivery().getPedidoAtual().isCartao()) {
            if (msg.getContent().toLowerCase().trim().contains("não") || msg.getContent().toLowerCase().trim().contains("nao") || msg.getContent().toLowerCase().trim().equals("n")) {
                getChatBotDelivery().getPedidoAtual().setTroco(0);
                chat.getChat().sendMessage("Beleza").join();
            } else {
                String valorTroco = "";
                for (char c : msg.getContent().trim().replaceAll(",", ".").toCharArray()) {
                    if (Character.isDigit(c) || c == '.') {
                        valorTroco += c;
                    }
                }
                try {
                    double valorTrocoDouble = Double.parseDouble(valorTroco);
                    if (valorTrocoDouble < getChatBotDelivery().getPedidoAtual().getTotal()) {
                        return false;
                    }
                    getChatBotDelivery().getPedidoAtual().setTroco(valorTrocoDouble);
                    chat.getChat().sendMessage("Beleza, já anotei aqui o valor para levar de troco").join();
                } catch (Exception ex) {
                    return false;
                }
            }
        } else {
            getChatBotDelivery().getPedidoAtual().setComentarioPedido(msg.getContent().trim());
            chat.getChat().sendMessage("Beleza, já anotei aqui").join();
        }
        chat.setHandler(new HandlerDesejaAgendar(chat), true);
        return true;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
