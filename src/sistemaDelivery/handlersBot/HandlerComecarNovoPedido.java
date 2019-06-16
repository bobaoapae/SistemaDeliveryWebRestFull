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

import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * @author jvbor
 */
public class HandlerComecarNovoPedido extends HandlerBotDelivery {

    private String[] agradecimentos = {"👍", "🤙", "🤝", "☺️"};

    public HandlerComecarNovoPedido(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        if (m.getContent().toLowerCase().contains("vlw") || m.getContent().toLowerCase().contains("obrigado")) {
            chat.getChat().sendMessage(agradecimentos[new Random().nextInt(agradecimentos.length - 1)]);
            return true;
        }
        chat.getChat().sendMessage("Olá, " + ((ChatBotDelivery) chat).getNome() + " 😄");
        chat.getChat().sendMessage("Gostaria de iniciar um novo pedido?");
        chat.getChat().sendMessage("1 - Sim");
        chat.getChat().sendMessage("2 - Não");
        return true;

    }

    @Override
    protected boolean runSecondTime(Message msg) {
        if (msg.getContent().trim().equals("1") || msg.getContent().toLowerCase().trim().equals("sim") || msg.getContent().toLowerCase().trim().equals("s")) {
            ((ChatBotDelivery) chat).setPedidoAtual(new Pedido(((ChatBotDelivery) chat).getCliente(), getChatBotDelivery().getEstabelecimento()));
            if (!getChatBotDelivery().getEstabelecimento().isOpenPedidos()) {
                if (getChatBotDelivery().getEstabelecimento().nextOrCurrentHorarioAbertoOfDay() == null) {
                    if (getChatBotDelivery().getEstabelecimento().checkTemHorarioFuncionamentoHoje()) {
                        chat.getChat().sendMessage("_Obs: Não realizamos atendimentos hoje_", 3500);
                    } else {
                        chat.getChat().sendMessage("_Obs: Já ecenrramos os atendimentos por hoje_", 3500);
                    }
                    chat.setHandler(new HandlerAdeus(chat), true);
                } else if (getChatBotDelivery().getEstabelecimento().isAgendamentoDePedidos()) {
                    chat.getChat().sendMessage("_Obs: Não iniciamos nosso atendimento ainda, porém você pode deixar seu pedido agendado._", 3000);
                    chat.setHandler(new HandlerMenuPrincipal(chat), true);
                } else if (getChatBotDelivery().getEstabelecimento().isReservas() && getChatBotDelivery().getEstabelecimento().isReservasComPedidosFechados()) {
                    chat.getChat().sendMessage("_Obs: Não iniciamos nosso atendimento ainda, nosso atendimento iniciasse às " + getChatBotDelivery().getEstabelecimento().nextOrCurrentHorarioAbertoOfDay().getHoraAbrir().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + ", porém você já pode realizar sua reserva de mesa_", 3500);
                    chat.setHandler(new HandlerDesejaFazerUmaReserva(chat), true);
                } else {
                    chat.getChat().sendMessage("_Obs: Não iniciamos nosso atendimento ainda, nosso atendimento iniciasse às " + getChatBotDelivery().getEstabelecimento().nextOrCurrentHorarioAbertoOfDay().getHoraAbrir().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + "._", 3500);
                    chat.setHandler(new HandlerAdeus(chat), true);
                }
            } else {
                chat.getChat().sendMessage("_Obs: Nosso prazo médio para entregas é de " + getChatBotDelivery().getEstabelecimento().getTempoMedioEntrega() + " à " + (getChatBotDelivery().getEstabelecimento().getTempoMedioEntrega() + 15) + " minutos. Já para retirada cerca de " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada()) + " à " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada() + 5) + " minutos._", 3000);
                chat.setHandler(new HandlerMenuPrincipal(chat), true);
            }
        } else if (msg.getContent().trim().equals("2") || msg.getContent().toLowerCase().trim().equals("não") || msg.getContent().toLowerCase().trim().equals("nao") || msg.getContent().toLowerCase().trim().equals("n")) {
            chat.setHandler(new HandlerAdeus(chat), true);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
