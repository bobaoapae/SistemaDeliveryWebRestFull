/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import handlersBot.HandlerBot;
import modelo.ChatBot;
import sistemaDelivery.modelo.ChatBotDelivery;

import java.text.DecimalFormat;

/**
 * @author jvbor
 */
public abstract class HandlerBotDelivery extends HandlerBot {

    protected DecimalFormat moneyFormat = new DecimalFormat("###,###,###.00");

    public HandlerBotDelivery(ChatBot chat) {
        super(chat);
    }

    public ChatBotDelivery getChatBotDelivery() {
        return (ChatBotDelivery) chat;
    }

    public abstract boolean notificaPedidosFechados();

}
