/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.ChatBotDelivery;

import java.time.format.DateTimeFormatter;

/**
 * @author jvbor
 */
public class HandlerDesejaAgendar extends HandlerBotDelivery {

    public HandlerDesejaAgendar(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        if (!getChatBotDelivery().getEstabelecimento().isOpenPedidos()) {
            if (getChatBotDelivery().getEstabelecimento().isAbrirFecharPedidosAutomaticamente()) {
                chat.getChat().sendMessage("Não iniciamos o atendimento ainda, nosso horário de atentimento é das " + getChatBotDelivery().getEstabelecimento().getHoraAutomaticaAbrirPedidos().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " às " + getChatBotDelivery().getEstabelecimento().getHoraAutomaticaFecharPedidos().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + ", porém você pode agendar o horario do seu pedido.", 1000);
            } else {
                chat.getChat().sendMessage("Não iniciamos o atendimento ainda, porém você pode agendar o horario do seu pedido.", 1000);
            }
            if (((ChatBotDelivery) chat).getPedidoAtual().isEntrega()) {
                chat.getChat().sendMessage("Você gostaria de agendar algum horario para o seu pedido ou quer que ele seja entregue assim que iniciarmos a produção e o pedido estiver pronto?", 1000);
                chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*", 500);
                chat.getChat().sendMessage("*1* - Agendar", 500);
                chat.getChat().sendMessage("*2* - Entregar assim que estiver pronto", 500);
            } else {
                chat.getChat().sendMessage("Você gostaria de agendar algum horario para a retirada do seu pedido ou podemos deixar ele pronto logo após iniciarmos a nossa produção?", 1000);
                chat.getChat().sendMessage("*1* - Agendar", 500);
                chat.getChat().sendMessage("*2* - Deixar pronto assim que iniciar a produção", 500);
            }
        } else {
            if (((ChatBotDelivery) chat).getPedidoAtual().isEntrega()) {
                chat.getChat().sendMessage("Você gostaria de agendar algum horario para o seu pedido ou quer que ele seja entregue assim que estiver pronto?", 1000);
                chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*");
                chat.getChat().sendMessage("*1* - Agendar", 500);
                chat.getChat().sendMessage("*2* - Entregar assim que estiver pronto", 500);
            } else {
                chat.getChat().sendMessage("Você gostaria de agendar algum horario para a retirada do seu pedido?");
                chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*");
                chat.getChat().sendMessage("*1* - Sim", 500);
                chat.getChat().sendMessage("*2* - Não", 500);
            }
        }
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        if (msg.getContent().trim().equals("1")) {
            chat.setHandler(new HandlerAgendamentoPedido(chat), true);
        } else if (msg.getContent().trim().equals("2")) {
            chat.setHandler(new HandlerConcluirPedido(chat), true);
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
