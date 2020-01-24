/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.TipoRecarga;

/**
 * @author jvbor
 */
public class HandlerPagarComCreditos extends HandlerBotDelivery {

    public HandlerPagarComCreditos(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        try {
            if (getChatBotDelivery().getCliente().getCreditosDisponiveis() >= getChatBotDelivery().getPedidoAtual().getTotal()) {
                getChatBotDelivery().getCliente().realizarRecarga(getChatBotDelivery().getEstabelecimento(), getChatBotDelivery().getCliente().getCreditosDisponiveis() - getChatBotDelivery().getPedidoAtual().getTotal(), TipoRecarga.SAQUE);
                getChatBotDelivery().getPedidoAtual().setPgCreditos(getChatBotDelivery().getPedidoAtual().getTotal());
            } else {
                getChatBotDelivery().getPedidoAtual().setPgCreditos(getChatBotDelivery().getCliente().getCreditosDisponiveis());
                getChatBotDelivery().getCliente().realizarRecarga(getChatBotDelivery().getEstabelecimento(), getChatBotDelivery().getCliente().getCreditosDisponiveis(), TipoRecarga.SAQUE);
            }
            if (getChatBotDelivery().getPedidoAtual().getTotal() == 0) {
                chat.getChat().sendMessage("Perfeito, seu pedido foi pago por completo utilizando seus créditos", 500).join();
                chat.getChat().sendMessage("Ainda lhe restaram R$" + moneyFormat.format(getChatBotDelivery().getCliente().getCreditosDisponiveis()) + " créditos", 2000).join();
                getChatBotDelivery().getPedidoAtual().setCartao(false);
                getChatBotDelivery().getPedidoAtual().setTroco(0);
                chat.setHandler(new HandlerDesejaAgendar(chat), true);
            } else if (getChatBotDelivery().getPedidoAtual().isEntrega()) {
                chat.getChat().sendMessage("Seu saldo de créditos foi insuficiente para pagar o pedido por completo, porém o valor que você tinha foi utilizado como desconto", 1000).join();
                chat.getChat().sendMessage("Ainda faltam R$" + moneyFormat.format(getChatBotDelivery().getPedidoAtual().getTotal()) + " a serem pagos", 5000).join();
                chat.setHandler(new HandlerSolicitarFormaPagamento(chat), true);
            } else {
                chat.getChat().sendMessage("Seu saldo de créditos foi insuficiente para pagar o pedido por completo, porém o valor que você tinha foi utilizado como desconto").join();
                chat.getChat().sendMessage("Ainda faltam R$" + moneyFormat.format(getChatBotDelivery().getPedidoAtual().getTotal()) + " a serem pagos", 5000).join();
                chat.setHandler(new HandlerDesejaAgendar(chat), true);
            }
        } catch (Exception e) {
            getChatBotDelivery().getChat().getDriver().onError(e);
            chat.getChat().sendMessage("Ouve um erro ao computar seus créditos tente novamente em alguns minutos ou aguarde o Atentende.").join();
            return false;
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
