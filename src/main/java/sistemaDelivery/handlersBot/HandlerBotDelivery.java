/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import handlersBot.HandlerBot;
import modelo.ChatBot;
import modelo.MessageBuilder;
import sistemaDelivery.modelo.ChatBotDelivery;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author jvbor
 */
public abstract class HandlerBotDelivery extends HandlerBot {

    protected ArrayList<OpcaoMenu> codigosMenu;
    protected DecimalFormat moneyFormat = new DecimalFormat("###,###,###.00");

    public HandlerBotDelivery(ChatBot chat) {
        super(chat);
        codigosMenu = new ArrayList<>();
    }

    protected final void addOpcaoMenu(HandlerBotDelivery handler, String titulo, String... keywords) {
        addOpcaoMenu(handler, titulo, null, keywords);
    }

    protected final void addOpcaoMenu(HandlerBotDelivery handler, String titulo, String subTitulo, String... keywords) {
        codigosMenu.add(new OpcaoMenu(handler, titulo, subTitulo, keywords));
    }

    public ChatBotDelivery getChatBotDelivery() {
        return (ChatBotDelivery) chat;
    }

    public abstract boolean notificaPedidosFechados();

    public class OpcaoMenu {
        private HandlerBotDelivery handlerBotDelivery;
        private String titulo;
        private String subTitulo;
        private List<String> keywords;

        public OpcaoMenu(HandlerBotDelivery handlerBotDelivery, String titulo, String subTitulo, String[] keywords) {
            this.handlerBotDelivery = handlerBotDelivery;
            this.titulo = titulo;
            this.subTitulo = subTitulo;
            this.keywords = Arrays.asList(keywords);
        }

        @Override
        public String toString() {
            int index = codigosMenu.indexOf(this) + 1;
            MessageBuilder msg = new MessageBuilder();
            msg.textNewLine("*" + index + "* - " + titulo);
            if (subTitulo != null && !subTitulo.isEmpty()) {
                msg.text("_" + subTitulo + "_");
            }
            return msg.build();
        }
    }

}
