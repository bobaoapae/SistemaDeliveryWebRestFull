/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import modelo.MessageBuilder;
import sistemaDelivery.modelo.AdicionalProduto;
import sistemaDelivery.modelo.GrupoAdicional;
import sistemaDelivery.modelo.ItemPedido;
import sistemaDelivery.modelo.Pedido;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author jvbor
 */
public class HandlerVerificaPedidoCorreto extends HandlerBotDelivery {

    public HandlerVerificaPedidoCorreto(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().markComposing(1500).join();
        chat.getChat().sendMessage("Vou mandar um resumo do seu pedido para que você verifique se está tudo certo, okay ☺️?!").join();
        Pedido p = getChatBotDelivery().getPedidoAtual();
        MessageBuilder builder = new MessageBuilder();
        for (int x = 0; x < p.getProdutos().size(); x++) {
            ItemPedido produto = p.getProdutos().get(x);
            builder.textNewLine(produto.getQtd() + "x - " + produto.getProduto().getNomeWithCategories() + (produto.getComentario().isEmpty() ? "" : " Obs: " + produto.getComentario()));
            Map<GrupoAdicional, List<AdicionalProduto>> hashMap = produto.getAdicionaisGroupByGrupo();
            for (Map.Entry<GrupoAdicional, List<AdicionalProduto>> entry : hashMap.entrySet()) {
                builder.textBold(entry.getKey().getNomeGrupo()).text(": ");
                String adicionais = "";
                for (int y = 0; y < entry.getValue().size(); y++) {
                    AdicionalProduto adicional = entry.getValue().get(y);
                    adicionais += adicional.getNome();
                    if (y < entry.getValue().size() - 1) {
                        adicionais += ", ";
                    }
                }
                if (adicionais.endsWith(", ")) {
                    adicionais = adicionais.substring(0, adicionais.lastIndexOf(", "));
                }
                builder.textNewLine(adicionais + ".");
            }
        }
        p.calcularValor();
        builder.textNewLine("Total: R$" + moneyFormat.format(p.getTotal()) + " 💵");
        chat.getChat().markComposing(5000).join();
        chat.getChat().sendMessage(builder.build()).join();
        chat.getChat().sendMessage("Está tudo certo? 🤞").join();
        addOpcaoSim(new HandlerFormaRetirada(chat), null);
        addOpcaoNao(new HandlerBoasVindas(chat), new Consumer<String>() {
            @Override
            public void accept(String s) {
                chat.getChat().sendMessage("Oh ☹️, sinto muito.").join();
                chat.getChat().sendMessage("Vamos começar novamente, espero que agora de tudo certo. 🤞😄").join();
            }
        });
        chat.getChat().sendMessage(gerarTextoOpcoes()).join();
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
