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
public class HandlerSolicitarQuantidadePessoasReserva extends HandlerBotDelivery {

    public HandlerSolicitarQuantidadePessoasReserva(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("A sua reserva seria para quantas pessoas?").join();
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        String qtdPessoasString = "";
        for (char c : msg.getContent().trim().replaceAll(" ", "").toCharArray()) {
            if (Character.isDigit(c)) {
                qtdPessoasString += c;
            }
        }
        try {
            int qtdPessoas = Integer.parseInt(qtdPessoasString);
            if (qtdPessoas == 0) {
                return false;
            }
            getChatBotDelivery().getReservaAtual().setQtdPessoas(qtdPessoas);
            chat.setHandler(new HandlerVerificaNomeContatoReserva(chat), true);
        } catch (NumberFormatException e) {
            getChatBotDelivery().getChat().getDriver().onError(e);
            return false;
        } catch (Exception ex) {
            getChatBotDelivery().getChat().getDriver().onError(ex);
            return false;
        }
        return true;
    }

    @Override
    protected void onError(Message m) {
        chat.getChat().sendMessage("A quantidade de pessoas informada é invalida, por favor informe novamente.").join();
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }


}
