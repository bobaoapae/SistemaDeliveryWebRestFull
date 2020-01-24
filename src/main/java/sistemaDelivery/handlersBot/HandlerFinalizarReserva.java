/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.controle.ControleReservas;

/**
 * @author jvbor
 */
public class HandlerFinalizarReserva extends HandlerBotDelivery {

    public HandlerFinalizarReserva(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        try {
            ControleReservas.getInstance().salvarReserva(getChatBotDelivery().getReservaAtual());
        } catch (Exception ex) {
            this.reset();
            chat.getChat().getDriver().onError(ex);
            chat.getChat().sendMessage("Falha ao registrar o pedido de reserva, tente novamente em alguns minutos").join();
            return true;
        }
        chat.getChat().sendMessage("Ótimo, seu pedido de reserva foi recebido. Agora aguarde o nosso contato para a confirmação da reserva!").join();
        chat.setHandler(new HandlerPedidoConcluido(chat), true);
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
