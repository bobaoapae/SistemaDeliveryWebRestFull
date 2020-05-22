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
public class HandlerComentarioPedido extends HandlerBotDelivery {

    private HandlerBotDelivery nextHandler;

    public HandlerComentarioPedido(ChatBot chat, HandlerBotDelivery nextHandler) {
        super(chat);
        this.nextHandler = nextHandler;
    }

    @Override
    protected boolean runFirstTime(Message m) {
        if (getChatBotDelivery().getLastPedido().getProduto().getCategoria().getExemplosComentarioPedido().equalsIgnoreCase("não")) {
            getChatBotDelivery().getLastPedido().setComentario("");
            chat.setHandler(nextHandler, true);
            return true;
        }
        chat.getChat().markComposing(2000).join();
        chat.getChat().sendMessage("Você deseja modificar algo em seu pedido?").join();
        if (getChatBotDelivery().getLastPedido().getProduto().getCategoria().getExemplosComentarioPedido() != null && !getChatBotDelivery().getLastPedido().getProduto().getCategoria().getExemplosComentarioPedido().isEmpty()) {
            chat.getChat().sendMessage("Por exemplo: " + getChatBotDelivery().getLastPedido().getProduto().getCategoria().getExemplosComentarioPedido() + "... etc").join();
        }
        chat.getChat().sendMessage("Basta escrever e me enviar, o que você escrever sera repassado para a àrea de produção").join();
        chat.getChat().sendMessage(gerarObs("Caso não queira modificar nada, basta enviar NÃO")).join();
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        getChatBotDelivery().setHandlerVoltar(null);
        if (msg.getContent().toLowerCase().trim().equals("não") || msg.getContent().toLowerCase().trim().equals("nao") || msg.getContent().toLowerCase().trim().equals("n")) {
            getChatBotDelivery().getLastPedido().setComentario("");
        } else {
            getChatBotDelivery().getLastPedido().setComentario(msg.getContent().trim());
            chat.getChat().sendMessage("Perfeito, já anotei aqui o que você me disse ✌️😉").join();
        }
        chat.setHandler(nextHandler, true);
        return true;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
