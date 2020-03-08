/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.Categoria;
import sistemaDelivery.modelo.ItemPedido;
import sistemaDelivery.modelo.TipoEntrega;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author jvbor
 */
public class HandlerFormaRetirada extends HandlerBotDelivery {


    public HandlerFormaRetirada(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        List<ItemPedido> pedidos = new ArrayList<>(getChatBotDelivery().getPedidoAtual().getProdutos());
        for (ItemPedido item : pedidos) {
            Categoria c = item.getProduto().getCategoria();
            if (!c.isFazEntrega() || !c.getRootCategoria().isFazEntrega()) {
                chat.setHandler(new HandlerRetiradaAutomatica(chat), true);
                return true;
            }
            if (c.getRootCategoria().getQtdMinEntrega() > getChatBotDelivery().getPedidoAtual().getProdutos(c).size() && !c.getRootCategoria().isPrecisaPedirOutraCategoria()) {
                chat.setHandler(new HandlerRetiradaAutomatica(chat), true);
                return true;
            } else if (c.getRootCategoria().isPrecisaPedirOutraCategoria()) {
                List<Categoria> categoriasCompradas = new ArrayList<>();
                for (ItemPedido item2 : getChatBotDelivery().getPedidoAtual().getProdutos()) {
                    if (!categoriasCompradas.contains(item2.getProduto().getCategoria().getRootCategoria())) {
                        categoriasCompradas.add(item2.getProduto().getCategoria().getRootCategoria());
                    }
                }
                categoriasCompradas.remove(item.getProduto().getCategoria().getRootCategoria());
                boolean temCategoriaPrecisa = false;
                for (Categoria catPrecisa : item.getProduto().getCategoria().getRootCategoria().getCategoriasNecessarias()) {
                    if (categoriasCompradas.contains(catPrecisa)) {
                        temCategoriaPrecisa = true;
                        break;
                    }
                }
                if (!temCategoriaPrecisa || c.getRootCategoria().getQtdMinEntrega() > getChatBotDelivery().getPedidoAtual().getProdutos(c).size()) {
                    chat.setHandler(new HandlerRetiradaAutomatica(chat), true);
                    return true;
                }
            }
        }
        chat.getChat().markComposing(1000).join();
        chat.getChat().sendMessage("Ótimo, agora só falta você me dizer como deseja retirar o seu pedido. 😁").join();
        String formasRetiradas = getChatBotDelivery().getEstabelecimento().getTiposEntregasConcatenados();
        boolean possuiEntrega = getChatBotDelivery().getEstabelecimento().possuiEntrega();
        synchronized (getChatBotDelivery().getEstabelecimento().getTiposEntregas()) {
            chat.getChat().markComposing(2500).join();
            if (possuiEntrega) {
                chat.getChat().sendMessage("Informo que nosso prazo médio para entrega é de " + getChatBotDelivery().getEstabelecimento().getTempoMedioEntrega() + " à " + (getChatBotDelivery().getEstabelecimento().getTempoMedioEntrega() + 15) + " minutos. Já para retirada cerca de " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada()) + " à " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada() + 5) + " minutos.").join();
            } else {
                chat.getChat().sendMessage("Informo que nosso prazo médio para retirada é de " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada()) + " à " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada() + 5) + " minutos.").join();
            }
            chat.getChat().markComposing(2000).join();
            chat.getChat().sendMessage("Você quer que seu pedido seja para " + formasRetiradas + "?").join();
            for (TipoEntrega tipoEntrega : getChatBotDelivery().getEstabelecimento().getTiposEntregas()) {
                boolean cobrarTaxa = tipoEntrega.getValor() > 0;
                if (tipoEntrega.getValor() > 0) {
                    for (ItemPedido itemPedido : getChatBotDelivery().getPedidoAtual().getProdutos()) {
                        if (itemPedido.getProduto().getCategoria().getRootCategoria().isEntregaGratis()) {
                            cobrarTaxa = false;
                            break;
                        }
                    }
                }
                addOpcaoMenu(null, new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                        getChatBotDelivery().getPedidoAtual().setTipoEntrega(tipoEntrega);
                        getChatBotDelivery().getPedidoAtual().calcularValor();
                        if (tipoEntrega.isSolicitarEndereco()) {
                            getChatBotDelivery().getPedidoAtual().setEntrega(true);
                            chat.getChat().sendMessage("Blz");
                            if (getChatBotDelivery().getCliente().getEndereco() == null || getChatBotDelivery().getCliente().getEndereco().getLogradouro().isEmpty()) {
                                chat.setHandler(new HandlerSolicitarEndereco(chat), true);
                            } else {
                                chat.setHandler(new HandlerUsarUltimoEndereco(chat), true);
                            }
                        } else {
                            getChatBotDelivery().getPedidoAtual().setEntrega(false);
                            try {
                                if (getChatBotDelivery().getCliente().getCreditosDisponiveis() > 0) {
                                    chat.setHandler(new HandlerDesejaUtilizarCreditos(chat), true);
                                } else {
                                    chat.setHandler(new HandlerDesejaAgendar(chat), true);
                                }
                            } catch (SQLException e) {
                                chat.getChat().getDriver().onError(e);
                                chat.setHandler(new HandlerDesejaAgendar(chat), true);
                            }
                        }
                    }
                }, tipoEntrega.getNome() + (cobrarTaxa ? " R$ " + getChatBotDelivery().getMoneyFormat().format(tipoEntrega.getValor()) : ""), "", tipoEntrega.getNome());
            }
            chat.getChat().sendMessage(gerarTextoOpcoes()).join();
        }
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        return processarOpcoesMenu(msg);
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
