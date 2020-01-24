/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import modelo.MessageBuilder;

import java.util.function.Consumer;

/**
 * @author jvbor
 */
public class HandlerUsarUltimoEndereco extends HandlerBotDelivery {

    public HandlerUsarUltimoEndereco(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        MessageBuilder builder = new MessageBuilder();
        builder.textNewLine("Tenho o seguinte endereço anotado aqui: *" + getChatBotDelivery().getCliente().getEndereco() + "*");
        builder.textNewLine("Gostaria de usar o mesmo endereço novamente?");
        builder.textNewLine(gerarObs("Olhe com atenção, pois caso esteja errado não vou conseguir realizar a entrega para você ☹"));
        builder.textNewLine(addOpcaoSim(new HandlerSolicitarFormaPagamento(chat), new Consumer<String>() {
            @Override
            public void accept(String s) {
                chat.getChat().sendMessage("Blz");
                getChatBotDelivery().getPedidoAtual().setEndereco(getChatBotDelivery().getCliente().getEndereco());
            }
        }).toString());
        builder.textNewLine(addOpcaoNao(new HandlerSolicitarEndereco(chat), null).toString());
        chat.getChat().markComposing(4000).join();
        chat.getChat().sendMessage(builder.build()).join();
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        return processarOpcoesMenu(msg);
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
