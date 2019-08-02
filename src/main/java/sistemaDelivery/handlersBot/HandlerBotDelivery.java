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
import java.util.ArrayList;

/**
 * @author jvbor
 */
public abstract class HandlerBotDelivery extends HandlerBot {

    protected DecimalFormat moneyFormat = new DecimalFormat("###,###,###.00");
    private int qtdObs;
    private final char[] numerosElevados = {'⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹'};

    public HandlerBotDelivery(ChatBot chat) {
        super(chat);
        codigosMenu = new ArrayList<>();
    }

    private String gerarNumeroElevado() {
        String numeroAtual = String.valueOf(qtdObs++);
        if (numeroAtual.equals("0")) {
            return "";
        }
        String numeroElevado = "";
        for (char n : numeroAtual.toCharArray()) {
            numeroElevado += numerosElevados[Integer.parseInt(Character.toString(n))];
        }
        return numeroElevado;
    }

    protected void resetarObs() {
        qtdObs = 0;
    }

    protected String gerarObs(String observacao) {
        return "*Obs" + gerarNumeroElevado() + "*: " + observacao;
    }

    public ChatBotDelivery getChatBotDelivery() {
        return (ChatBotDelivery) chat;
    }

    public abstract boolean notificaPedidosFechados();

}
