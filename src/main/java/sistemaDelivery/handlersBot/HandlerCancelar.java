/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.Chat;
import modelo.ChatBot;
import modelo.Message;

/**
 * @author jvbor
 */
public class HandlerCancelar extends HandlerBotDelivery {

    public HandlerCancelar(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().markComposing(2500);
        chat.getChat().sendMessage("O atendimento foi cancelado.");
        chat.setHandler(new HandlerAdeus(chat), true);
        chat.getChat().getDriver().runOnDriverThreads(() -> {
            try {
                Chat c = chat.getChat().getDriver().getFunctions().getChatByNumber("554491050665");
                if (c != null) {
                    c.sendMessage("*" + getChatBotDelivery().getEstabelecimento().getNomeEstabelecimento() + ":* Pedido cancelado para o cliente " + getChatBotDelivery().getNome());
                    c.sendFile(c.printScreen(), "Pedido Cancelado");
                    Thread.sleep(3000);
                    c.setArchive(true);
                }
            } catch (Exception ignored) {

            }
        });
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        return runFirstTime(m);
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
