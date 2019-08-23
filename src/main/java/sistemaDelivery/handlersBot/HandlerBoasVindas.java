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
import sistemaDelivery.modelo.Pedido;

/**
 * @author jvbor
 */
public class HandlerBoasVindas extends HandlerBotDelivery {

    public HandlerBoasVindas(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        int horaAtual = getChatBotDelivery().getEstabelecimento().getHoraAtual().getHour();
        String msg = "";
        if (horaAtual >= 2 && horaAtual < 12) {
            msg = "Bom Dia";
        } else if (horaAtual >= 12 && horaAtual < 18) {
            msg = "Boa Tarde";
        } else {
            msg = "Boa Noite";
        }
        MessageBuilder builder = new MessageBuilder();
        builder.text(msg).text(" ").text(((ChatBotDelivery) chat).getNome()).text(".").newLine();
        builder.textNewLine("Eu sou o " + getChatBotDelivery().getEstabelecimento().getNomeBot() + ", atendente virtual da " + getChatBotDelivery().getEstabelecimento().getNomeEstabelecimento() + ", e irei te ajudar a completar seu pedido.").
                textNewLine("*_Lembre-se de ler as instruções com atenção_*");
        ((ChatBotDelivery) chat).setPedidoAtual(new Pedido(((ChatBotDelivery) chat).getCliente(), getChatBotDelivery().getEstabelecimento()));
        chat.getChat().sendMessage(builder.build());
        getChatBotDelivery().enviarMensageInformesIniciais();
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
