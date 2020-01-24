/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;

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
        if (!getChatBotDelivery().getEstabelecimento().isAgendamentoDePedidos()) {
            chat.setHandler(new HandlerConcluirPedido(chat), true);
            return true;
        }
        if (!getChatBotDelivery().getEstabelecimento().isOpenPedidos()) {
            chat.getChat().markComposing(3000).join();
            if (getChatBotDelivery().getEstabelecimento().nextHorarioAbertoOfDay() != null) {
                chat.getChat().sendMessage("Não iniciamos o atendimento ainda, nosso horário de atentimento é das " + getChatBotDelivery().getEstabelecimento().nextHorarioAbertoOfDay().getHoraAbrir().format(DateTimeFormatter.ofPattern("HH:mm")) + " às " + getChatBotDelivery().getEstabelecimento().nextHorarioAbertoOfDay().getHoraFechar().format(DateTimeFormatter.ofPattern("HH:mm")) + ", porém você pode agendar o horario do seu pedido.").join();
            } else {
                chat.getChat().sendMessage("Não iniciamos o atendimento ainda, porém você pode agendar o horario do seu pedido.").join();
            }
            if (getChatBotDelivery().getPedidoAtual().isEntrega()) {
                chat.getChat().sendMessage("Você gostaria de agendar algum horario para o seu pedido ou quer que ele seja entregue assim que iniciarmos a produção e o pedido estiver pronto?").join();
                addOpcaoMenu(new HandlerAgendamentoPedido(chat), null, "Agendar", "", "agendar");
                addOpcaoMenu(new HandlerConcluirPedido(chat), null, "Entregar assim que estiver pronto", "", "quando", "pronto", "estiver");
            } else {
                chat.getChat().sendMessage("Você gostaria de agendar algum horario para a retirada do seu pedido ou podemos deixar ele pronto logo após iniciarmos a nossa produção?").join();
                addOpcaoMenu(new HandlerAgendamentoPedido(chat), null, "Agendar", "", "agendar");
                addOpcaoMenu(new HandlerConcluirPedido(chat), null, "Deixar pronto assim que iniciar a produção", "", "quando", "iniciar", "produção");
            }
        } else {
            if (getChatBotDelivery().getPedidoAtual().isEntrega()) {
                chat.getChat().sendMessage("Você gostaria de agendar algum horario para o seu pedido ou quer que ele seja entregue assim que estiver pronto?").join();
                addOpcaoMenu(new HandlerAgendamentoPedido(chat), null, "Agendar", "", "agendar");
                addOpcaoMenu(new HandlerConcluirPedido(chat), null, "Entregar assim que estiver pronto", "", "quando", "pronto", "estiver");
            } else {
                chat.getChat().sendMessage("Você gostaria de agendar algum horario para a retirada do seu pedido?").join();
                addOpcaoSim(new HandlerAgendamentoPedido(chat), null);
                addOpcaoNao(new HandlerConcluirPedido(chat), null);
            }
        }
        chat.getChat().markComposing(2500).join();
        chat.getChat().sendMessage(gerarTextoOpcoes()).join();
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
