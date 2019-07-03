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
            this.reset();
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
            getChatBotDelivery().enviarMensageInformesIniciais();
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
