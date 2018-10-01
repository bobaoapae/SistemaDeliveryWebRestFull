/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.ChatBotDelivery;
import sistemaDelivery.modelo.ItemPedido;

/**
 * @author jvbor
 */
public class HandlerFormaRetirada extends HandlerBotDelivery {

    public HandlerFormaRetirada(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Informo que nosso prazo médio para entrega é de " + getChatBotDelivery().getEstabelecimento().getTempoMedioEntrega() + " à " + (getChatBotDelivery().getEstabelecimento().getTempoMedioEntrega() + 15) + " minutos. Já para retirada cerca de " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada()) + " à " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada() + 5) + " minutos.", 2000);
        chat.getChat().sendMessage("Você quer que seu pedido seja para entrega ou retirada no balcão?");
        chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*");
        if (((ChatBotDelivery) chat).getEstabelecimento().getTaxaEntregaFixa() != 0 || ((ChatBotDelivery) chat).getEstabelecimento().getTaxaEntregaKm() != 0) {
            boolean cobrarTaxa = true;
            for (ItemPedido itemPedido : ((ChatBotDelivery) chat).getPedidoAtual().getProdutos()) {
                if (itemPedido.getProduto().getCategoria().getRootCategoria().isEntregaGratis()) {
                    cobrarTaxa = true;
                    break;
                }
            }
            if (cobrarTaxa) {
                chat.getChat().sendMessage("*1* - 🛵 Entrega R$ " + ((ChatBotDelivery) chat).getMoneyFormat().format(((ChatBotDelivery) chat).getEstabelecimento().getTaxaEntregaFixa()));
            } else {
                chat.getChat().sendMessage("*1* - 🛵 Entrega");
            }
        } else {
            chat.getChat().sendMessage("*1* - 🛵 Entrega");
        }
        chat.getChat().sendMessage("*2* - 🛎️ Retirada no balcão");
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        if (msg.getContent().trim().equals("1") || msg.getContent().toLowerCase().trim().contains("entrega")) {
            ((ChatBotDelivery) chat).getPedidoAtual().setEntrega(true);
            chat.getChat().sendMessage("Blz");
            if (((ChatBotDelivery) chat).getCliente().getEndereco() == null || ((ChatBotDelivery) chat).getCliente().getEndereco().getLogradouro().isEmpty()) {
                chat.setHandler(new HandlerSolicitarEndereco(chat), true);
            } else {
                chat.setHandler(new HandlerUsarUltimoEndereco(chat), true);
            }
        } else if (msg.getContent().trim().equals("2") || msg.getContent().toLowerCase().trim().contains("retira") || msg.getContent().toLowerCase().trim().contains("busca")) {
            ((ChatBotDelivery) chat).getPedidoAtual().setEntrega(false);
            if (((ChatBotDelivery) chat).getCliente().getCreditosDisponiveis(getChatBotDelivery().getEstabelecimento()) > 0) {
                chat.setHandler(new HandlerDesejaUtilizarCreditos(chat), true);
            } else {
                chat.setHandler(new HandlerDesejaAgendar(chat), true);
            }
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
