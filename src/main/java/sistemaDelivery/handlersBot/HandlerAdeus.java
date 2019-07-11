/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.ChatBotDelivery;
import sistemaDelivery.modelo.Pedido;

/**
 * @author jvbor
 */
public class HandlerAdeus extends HandlerBotDelivery {

    public HandlerAdeus(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        getChatBotDelivery().setHandlerVoltar(null);
        ((ChatBotDelivery) chat).setPedidoAtual(new Pedido(((ChatBotDelivery) chat).getCliente(), getChatBotDelivery().getEstabelecimento()));
        chat.setHandler(new HandlerComecarNovoPedido(chat), false);
        chat.getChat().sendMessage("Até mais, " + ((ChatBotDelivery) chat).getNome() + ". Obrigado pela preferência");
        chat.getChat().sendMessage("Aguardamos seu retorno 🤗🖤");
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
