package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import modelo.MessageBuilder;
import sistemaDelivery.modelo.TipoEntrega;

import java.sql.SQLException;

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
        getChatBotDelivery().getPedidoAtual().setEntrega(false);
        synchronized (getChatBotDelivery().getEstabelecimento().getTiposEntregas()) {
            for (TipoEntrega tipoEntrega : getChatBotDelivery().getEstabelecimento().getTiposEntregas()) {
                if (!tipoEntrega.isSolicitarEndereco() && tipoEntrega.getValor() == 0) {
                    getChatBotDelivery().getPedidoAtual().setTipoEntrega(tipoEntrega);
                    break;
                }
            }
        }
        if (getChatBotDelivery().getEstabelecimento().possuiEntrega()) {
            chat.getChat().sendMessage("O seu pedido foi marcado automaticamente como para retirada, pois algum produto que você pediu não pode ser entregue.").join();
            chat.getChat().sendMessage("Deseja prosseguir com o pedido?").join();
            chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*").join();
            MessageBuilder builder = new MessageBuilder();
            builder.textNewLine("*1* - Sim.").
                    textNewLine("*2* - Não.");
            chat.getChat().sendMessage(builder.build()).join();
        } else {
            irParaProximaEtapa();
        }
        return true;
    }

    @Override
    protected boolean runSecondTime(Message message) {
        if (message.getContent().trim().equals("1")) {
            irParaProximaEtapa();
            return true;
        } else if (message.getContent().trim().equals("2")) {
            chat.setHandler(new HandlerAdeus(chat), true);
            return true;
        }
        return false;
    }

    private void irParaProximaEtapa() {
        try {
            if (getChatBotDelivery().getCliente().getCreditosDisponiveis() > 0) {
                chat.setHandler(new HandlerDesejaUtilizarCreditos(chat), true);
            } else {
                chat.setHandler(new HandlerDesejaAgendar(chat), true);
            }
        } catch (SQLException e) {
            getChatBotDelivery().getChat().getDriver().onError(e);
            chat.setHandler(new HandlerDesejaAgendar(chat), true);
        }
    }
}
