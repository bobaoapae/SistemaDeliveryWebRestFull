/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.ChatBotDelivery;

/**
 * @author jvbor
 */
public class HandlerPagarComCreditos extends HandlerBotDelivery {

    public HandlerPagarComCreditos(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        if (((ChatBotDelivery) chat).getCliente().getCreditosDisponiveis(getChatBotDelivery().getEstabelecimento()) >= ((ChatBotDelivery) chat).getPedidoAtual().getTotal()) {
            ((ChatBotDelivery) chat).getCliente().realizarRecarga(getChatBotDelivery().getEstabelecimento(), -((ChatBotDelivery) chat).getCliente().getCreditosDisponiveis(getChatBotDelivery().getEstabelecimento()) - ((ChatBotDelivery) chat).getPedidoAtual().getTotal());
            ((ChatBotDelivery) chat).getPedidoAtual().setPgCreditos(((ChatBotDelivery) chat).getPedidoAtual().getTotal());
        } else {
            ((ChatBotDelivery) chat).getPedidoAtual().setPgCreditos(((ChatBotDelivery) chat).getCliente().getCreditosDisponiveis(getChatBotDelivery().getEstabelecimento()));
            ((ChatBotDelivery) chat).getCliente().realizarRecarga(getChatBotDelivery().getEstabelecimento(), -((ChatBotDelivery) chat).getCliente().getCreditosDisponiveis(getChatBotDelivery().getEstabelecimento()));
        }
        if (((ChatBotDelivery) chat).getPedidoAtual().getTotal() == 0) {
            chat.getChat().sendMessage("Perfeito, seu pedido foi pago por completo utilizando seus créditos", 500);
            chat.getChat().sendMessage("Ainda lhe restaram R$" + moneyFormat.format(((ChatBotDelivery) chat).getCliente().getCreditosDisponiveis(getChatBotDelivery().getEstabelecimento())) + " créditos", 2000);
            ((ChatBotDelivery) chat).getPedidoAtual().setCartao(false);
            ((ChatBotDelivery) chat).getPedidoAtual().setTroco(0);
            chat.setHandler(new HandlerDesejaAgendar(chat), true);
        } else if (((ChatBotDelivery) chat).getPedidoAtual().isEntrega()) {
            chat.getChat().sendMessage("Seu saldo de créditos foi insuficiente para pagar o pedido por completo, porém o valor que você tinha foi utilizado como desconto", 1000);
            chat.getChat().sendMessage("Ainda faltam R$" + moneyFormat.format(((ChatBotDelivery) chat).getPedidoAtual().getTotal()) + " a serem pagos", 5000);
            chat.setHandler(new HandlerSolicitarFormaPagamento(chat), true);
        } else {
            chat.getChat().sendMessage("Seu saldo de créditos foi insuficiente para pagar o pedido por completo, porém o valor que você tinha foi utilizado como desconto");
            chat.getChat().sendMessage("Ainda faltam R$" + moneyFormat.format(((ChatBotDelivery) chat).getPedidoAtual().getTotal()) + " a serem pagos", 5000);
            chat.setHandler(new HandlerDesejaAgendar(chat), true);
        }
        try {
            //ControleClientes.getInstance(Db4oGenerico.getInstance("banco")).alterar(((ChatBotDelivery) chat).getCliente());
        } catch (Exception ex) {
            chat.getChat().getDriver().onError(ex);
        }
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        return true;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
