package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;

public class HandlerRepetirUltimoItemPedido extends HandlerBotDelivery {


    public HandlerRepetirUltimoItemPedido(ChatBot chat) {
        super(chat);
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

    @Override
    protected boolean runFirstTime(Message message) {
        getChatBotDelivery().getLastPedido().setQtd(getChatBotDelivery().getLastPedido().getQtd() + 1);
        return true;
    }

    @Override
    protected boolean runSecondTime(Message message) {
        return runFirstTime(message);
    }
}
