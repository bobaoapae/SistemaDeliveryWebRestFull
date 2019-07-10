/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import modelo.MessageBuilder;
import sistemaDelivery.modelo.ChatBotDelivery;

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
        builder.textNewLine("Tenho o seguinte endereço anotado aqui: *" + ((ChatBotDelivery) chat).getCliente().getEndereco() + "*");
        builder.textNewLine("Gostaria de usar o mesmo endereço novamente?");
        builder.textNewLine("*_Obs¹: Envie somente o número da sua escolha_*");
        builder.textNewLine("*_Obs²:Olhe com atenção, pois caso esteja errado não vou conseguir realizar a entrega para você_* ☹️");
        builder.textNewLine("*1* - Sim");
        builder.textNewLine("*2* - Não");
        chat.getChat().sendMessage(builder.build());
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        if (msg.getContent().trim().equals("1") || msg.getContent().toLowerCase().trim().equals("sim") || msg.getContent().toLowerCase().trim().equals("s")) {
            chat.getChat().sendMessage("Blz");
            ((ChatBotDelivery) chat).getPedidoAtual().setEndereco(((ChatBotDelivery) chat).getCliente().getEndereco());
            chat.setHandler(new HandlerSolicitarFormaPagamento(chat), true);
        } else if (msg.getContent().trim().equals("2") || msg.getContent().toLowerCase().trim().equals("não") || msg.getContent().toLowerCase().trim().equals("nao") || msg.getContent().toLowerCase().trim().equals("n")) {
            chat.setHandler(new HandlerSolicitarEndereco(chat), true);
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
