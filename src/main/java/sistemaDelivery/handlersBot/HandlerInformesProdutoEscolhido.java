/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.Categoria;
import sistemaDelivery.modelo.ChatBotDelivery;
import sistemaDelivery.modelo.ItemPedido;
import sistemaDelivery.modelo.Produto;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jvbor
 */
public class HandlerInformesProdutoEscolhido extends HandlerBotDelivery {

    private HandlerBotDelivery nextHandler;
    private Produto produtoEscolhido;

    public HandlerInformesProdutoEscolhido(Produto produtoEscolhido, ChatBot chat, HandlerBotDelivery nextHandler) {
        super(chat);
        this.nextHandler = nextHandler;
        this.produtoEscolhido = produtoEscolhido;
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Você escolheu: " + this.produtoEscolhido.getNomeWithCategories());
        chat.getChat().sendMessage("*_Obs¹: Caso a escolha esteja incorreta envie: VOLTAR_*", 1500);
        getChatBotDelivery().setHandlerVoltar(new HandlerVoltar(new HandlerMenuCategoria(produtoEscolhido.getCategoria(), chat), new Runnable() {
            @Override
            public void run() {
                getChatBotDelivery().setLastPedido(null);
            }
        }, false));
        boolean flagMsg = false;
        if (getChatBotDelivery().getEstabelecimento().possuiEntrega() && (!this.produtoEscolhido.getCategoria().isFazEntrega() || !this.produtoEscolhido.getCategoria().getRootCategoria().isFazEntrega())) {
            chat.getChat().sendMessage("*_Obs²: Não é feita a entrega do produto escolhido_*", 3000);
        } else {
            List<ItemPedido> pedidos = new ArrayList<>(((ChatBotDelivery) chat).getPedidoAtual().getProdutos());
            for (ItemPedido item : pedidos) {
                Categoria c = item.getProduto().getCategoria();
                if (!c.isFazEntrega() || !c.getRootCategoria().isFazEntrega()) {
                    flagMsg = true;
                }
                if (c.getRootCategoria().getQtdMinEntrega() > ((ChatBotDelivery) chat).getPedidoAtual().getProdutos(c).size() && !c.getRootCategoria().isPrecisaPedirOutraCategoria()) {
                    flagMsg = true;
                } else {
                    List<Categoria> categoriasCompradas = new ArrayList<>();
                    for (ItemPedido item2 : ((ChatBotDelivery) chat).getPedidoAtual().getProdutos()) {
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
                    if (!temCategoriaPrecisa || c.getRootCategoria().getQtdMinEntrega() > ((ChatBotDelivery) chat).getPedidoAtual().getProdutos(c).size()) {
                        flagMsg = true;
                    }
                }
            }
            if (flagMsg) {
                String categoriasComprar = "";
                for (Categoria c : this.produtoEscolhido.getCategoria().getRootCategoria().getCategoriasNecessarias()) {
                    categoriasComprar += c.getNomeCategoria() + ",";
                }
                if (!categoriasComprar.isEmpty()) {
                    categoriasComprar = categoriasComprar.substring(0, categoriasComprar.lastIndexOf(","));
                }
                if (this.produtoEscolhido.getCategoria().getRootCategoria().getQtdMinEntrega() > 1 && !this.produtoEscolhido.getCategoria().getRootCategoria().isPrecisaPedirOutraCategoria()) {
                    chat.getChat().sendMessage("*_Obs²: A entrega só e feita se você pedir no minimo " + this.produtoEscolhido.getCategoria().getRootCategoria().getQtdMinEntrega() + " itens_*", 3000);
                } else if (this.produtoEscolhido.getCategoria().getRootCategoria().getQtdMinEntrega() > 1 && this.produtoEscolhido.getCategoria().getRootCategoria().isPrecisaPedirOutraCategoria()) {
                    chat.getChat().sendMessage("*_Obs²: A entrega só e feita se você pedir no minimo " + this.produtoEscolhido.getCategoria().getRootCategoria().getQtdMinEntrega() + " itens ou pedir junto algum produto de outro cardapio (" + categoriasComprar + ")_*", 3000);
                } else if (this.produtoEscolhido.getCategoria().getRootCategoria().isPrecisaPedirOutraCategoria()) {
                    chat.getChat().sendMessage("*_Obs²: A entrega só e feita se você pedir junto algum produto de outro cardapio (" + categoriasComprar + ")_*", 3000);
                }
            }
        }
        ItemPedido item = new ItemPedido();
        item.setProduto(produtoEscolhido);
        ((ChatBotDelivery) chat).setLastPedido(item);
        chat.setHandler(nextHandler, true);
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        return runFirstTime(msg);
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
