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
import sistemaDelivery.modelo.TipoEntrega;

import java.util.ArrayList;

/**
 * @author jvbor
 */
public class HandlerFormaRetirada extends HandlerBotDelivery {

    private ArrayList<TipoEntrega> codigosMenu = new ArrayList<>();

    public HandlerFormaRetirada(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        codigosMenu.clear();
        String formasRetiradas = "";
        synchronized (getChatBotDelivery().getEstabelecimento().getTiposEntregas()) {

            boolean possuiEntrega = false;

            for (TipoEntrega tipoEntrega : getChatBotDelivery().getEstabelecimento().getTiposEntregas()) {
                formasRetiradas += tipoEntrega.getNome() + ", ";
                if (tipoEntrega.isSolicitarEndereco()) {
                    possuiEntrega = true;
                    break;
                }
            }

            formasRetiradas = formasRetiradas.trim().substring(0, formasRetiradas.lastIndexOf(","));
            if (formasRetiradas.contains(", ")) {
                formasRetiradas = formasRetiradas.substring(0, formasRetiradas.lastIndexOf(",")) + " ou" + formasRetiradas.substring(formasRetiradas.lastIndexOf(",") + 1);
            }

            if (possuiEntrega) {
                chat.getChat().sendMessage("Informo que nosso prazo médio para entrega é de " + getChatBotDelivery().getEstabelecimento().getTempoMedioEntrega() + " à " + (getChatBotDelivery().getEstabelecimento().getTempoMedioEntrega() + 15) + " minutos. Já para retirada cerca de " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada()) + " à " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada() + 5) + " minutos.", 2000);
            } else {
                chat.getChat().sendMessage("Informo que nosso prazo médio para retirada é de " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada()) + " à " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada() + 5) + " minutos.", 2000);
            }

            chat.getChat().sendMessage("Você quer que seu pedido seja para " + formasRetiradas + "?");
            chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*");

            for (TipoEntrega tipoEntrega : getChatBotDelivery().getEstabelecimento().getTiposEntregas()) {
                boolean cobrarTaxa = tipoEntrega.getValor() > 0;
                if (tipoEntrega.getValor() > 0) {
                    for (ItemPedido itemPedido : ((ChatBotDelivery) chat).getPedidoAtual().getProdutos()) {
                        if (itemPedido.getProduto().getCategoria().getRootCategoria().isEntregaGratis()) {
                            cobrarTaxa = false;
                            break;
                        }
                    }
                }
                codigosMenu.add(tipoEntrega);
                if (cobrarTaxa) {
                    chat.getChat().sendMessage("*" + codigosMenu.size() + "* - " + tipoEntrega.getNome() + " R$ " + ((ChatBotDelivery) chat).getMoneyFormat().format(tipoEntrega.getValor()));
                } else {
                    chat.getChat().sendMessage("*" + codigosMenu.size() + "* - " + tipoEntrega.getNome());
                }
            }
        }
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        try {
            int escolha = Integer.parseInt(msg.getContent().trim()) - 1;
            if (escolha >= 0 && codigosMenu.size() > escolha) {
                TipoEntrega tipoEntrega = codigosMenu.get(escolha);
                ((ChatBotDelivery) chat).getPedidoAtual().setTipoEntrega(tipoEntrega);
                if (tipoEntrega.isSolicitarEndereco()) {
                    ((ChatBotDelivery) chat).getPedidoAtual().setEntrega(true);
                    chat.getChat().sendMessage("Blz");
                    if (((ChatBotDelivery) chat).getCliente().getEndereco() == null || ((ChatBotDelivery) chat).getCliente().getEndereco().getLogradouro().isEmpty()) {
                        chat.setHandler(new HandlerSolicitarEndereco(chat), true);
                    } else {
                        chat.setHandler(new HandlerUsarUltimoEndereco(chat), true);
                    }
                } else {
                    ((ChatBotDelivery) chat).getPedidoAtual().setEntrega(false);
                    if (((ChatBotDelivery) chat).getCliente().getCreditosDisponiveis() > 0) {
                        chat.setHandler(new HandlerDesejaUtilizarCreditos(chat), true);
                    } else {
                        chat.setHandler(new HandlerDesejaAgendar(chat), true);
                    }
                }
                return true;
            } else {
                return false;
            }
        } catch (NumberFormatException e) {
            getChatBotDelivery().getChat().getDriver().onError(e);
            return false;
        } catch (Exception ex) {
            getChatBotDelivery().getChat().getDriver().onError(ex);
            return false;
        }
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
