/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.Pedido;

import java.util.function.Consumer;

/**
 * @author jvbor
 */
public class HandlerPedidoConcluido extends HandlerBotDelivery {

    public HandlerPedidoConcluido(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Existe algo mais em que eu possa lhe ajudar?");
        addOpcaoSim(new HandlerMenuPrincipal(chat), new Consumer<String>() {
            @Override
            public void accept(String s) {
                getChatBotDelivery().setPedidoAtual(new Pedido(getChatBotDelivery().getCliente(), getChatBotDelivery().getEstabelecimento()));
            }
        });
        HandlerBotDelivery handlerBotDelivery = null;
        if (getChatBotDelivery().getCliente().isCadastroRealizado()) {
            handlerBotDelivery = new HandlerAdeus(chat);
        } else {
            handlerBotDelivery = new HandlerVerificaDesejaRealizarCadastro(chat);
        }
        addOpcaoNao(handlerBotDelivery, null);
        chat.getChat().sendMessage(gerarTextoOpcoes());
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        return processarOpcoesMenu(msg);
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
