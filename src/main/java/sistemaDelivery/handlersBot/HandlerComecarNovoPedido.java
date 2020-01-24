/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.Pedido;

import java.util.Random;
import java.util.function.Consumer;

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
            chat.getChat().sendMessage(agradecimentos[new Random().nextInt(agradecimentos.length - 1)]).join();
            this.reset();
            return true;
        }
        chat.getChat().sendMessage("Olá, " + getChatBotDelivery().getNome() + " 😄").join();
        chat.getChat().sendMessage("Gostaria de iniciar um novo pedido?").join();
        addOpcaoSim(null, new Consumer<String>() {
            @Override
            public void accept(String s) {
                getChatBotDelivery().setPedidoAtual(new Pedido(getChatBotDelivery().getCliente(), getChatBotDelivery().getEstabelecimento()));
                getChatBotDelivery().enviarMensageInformesIniciais();
            }
        });
        addOpcaoNao(new HandlerAdeus(chat), null);
        chat.getChat().sendMessage(gerarTextoOpcoes()).join();
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
