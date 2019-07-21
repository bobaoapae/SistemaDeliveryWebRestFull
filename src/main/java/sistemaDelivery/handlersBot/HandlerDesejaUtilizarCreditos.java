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
public class HandlerDesejaUtilizarCreditos extends HandlerBotDelivery {

    public HandlerDesejaUtilizarCreditos(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        try {
            chat.getChat().sendMessage("Você possui R$ " + moneyFormat.format(getChatBotDelivery().getCliente().getCreditosDisponiveis()) + " de créditos", 500);
        } catch (SQLException e) {
            getChatBotDelivery().getChat().getDriver().onError(e);
        }
        chat.getChat().sendMessage("Deseja utlizar o valor como desconto?", 300);
        chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*");
        chat.getChat().sendMessage("1 - Sim");
        chat.getChat().sendMessage("2 - Não");
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        if (msg.getContent().trim().equals("1") || msg.getContent().toLowerCase().trim().contains("sim")) {
            chat.setHandler(new HandlerPagarComCreditos(chat), true);
        } else if (msg.getContent().trim().equals("2") || msg.getContent().toLowerCase().trim().contains("não")) {
            chat.setHandler(new HandlerDesejaAgendar(chat), true);
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
