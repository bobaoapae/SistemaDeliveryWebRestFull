package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import modelo.MessageBuilder;
import sistemaDelivery.modelo.ChatBotDelivery;
import sistemaDelivery.modelo.TipoEntrega;

public class HandlerRetiradaAutomatica extends HandlerBotDelivery {
    public HandlerRetiradaAutomatica(ChatBot chat) {
        super(chat);
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

    @Override
    protected boolean runFirstTime(Message message) {
        ((ChatBotDelivery) chat).getPedidoAtual().setEntrega(false);
        synchronized (getChatBotDelivery().getEstabelecimento().getTiposEntregas()) {
            for (TipoEntrega tipoEntrega : getChatBotDelivery().getEstabelecimento().getTiposEntregas()) {
                if (!tipoEntrega.isSolicitarEndereco() && tipoEntrega.getValor() == 0) {
                    ((ChatBotDelivery) chat).getPedidoAtual().setTipoEntrega(tipoEntrega);
                    break;
                }
            }
        }
        chat.getChat().sendMessage("O seu pedido foi marcado automaticamente como para retirada, pois algum produto que você pediu não pode ser entregue.", 2000);
        chat.getChat().sendMessage("Deseja prosseguir com o pedido?");
        chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*");
        MessageBuilder builder = new MessageBuilder();
        builder.textNewLine("*1* - Sim.").
                textNewLine("*2* - Não.");
        chat.getChat().sendMessage(builder.build());
        return true;
    }

    @Override
    protected boolean runSecondTime(Message message) {
        if (message.getContent().trim().equals("1")) {
            if (((ChatBotDelivery) chat).getCliente().getCreditosDisponiveis() > 0) {
                chat.setHandler(new HandlerDesejaUtilizarCreditos(chat), true);
            } else {
                chat.setHandler(new HandlerDesejaAgendar(chat), true);
            }
            return true;
        } else if (message.getContent().trim().equals("2")) {
            chat.setHandler(new HandlerAdeus(chat), true);
            return true;
        }
        return false;
    }
}
