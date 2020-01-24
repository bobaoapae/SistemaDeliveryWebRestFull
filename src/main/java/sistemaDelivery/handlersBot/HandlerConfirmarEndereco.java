/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import modelo.MessageBuilder;
import sistemaDelivery.controle.ControleClientes;

import java.util.function.Consumer;

/**
 * @author jvbor
 */
public class HandlerConfirmarEndereco extends HandlerBotDelivery {

    public HandlerConfirmarEndereco(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        MessageBuilder builder = new MessageBuilder();
        builder.textNewLine("Anotei o seguinte endereço aqui: *" + getChatBotDelivery().getPedidoAtual().getEndereco() + "*");
        builder.textNewLine("O endereço informado está correto?");
        builder.textNewLine(gerarObs("*_Olhe com atenção, pois caso esteja errado não vou conseguir realizar a entrega para você_* ☹"));
        builder.textNewLine(addOpcaoSim(new HandlerSolicitarFormaPagamento(chat), new Consumer<String>() {
            @Override
            public void accept(String s) {
                chat.getChat().sendMessage("Blz");
                getChatBotDelivery().getCliente().setEndereco(getChatBotDelivery().getPedidoAtual().getEndereco());
                try {
                    ControleClientes.getInstance().salvarCliente(getChatBotDelivery().getCliente());
                } catch (Exception ex) {
                    chat.getChat().getDriver().onError(ex);
                }
            }
        }).toString());
        builder.textNewLine(addOpcaoNao(new HandlerSolicitarEndereco(chat), new Consumer<String>() {
            @Override
            public void accept(String s) {
                chat.getChat().sendMessage("Sinto muito, vou anotar seu endereço novamente então");
            }
        }).toString());
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
