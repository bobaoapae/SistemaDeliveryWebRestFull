/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import modelo.MessageBuilder;
import sistemaDelivery.controle.ControleCategorias;
import sistemaDelivery.modelo.Categoria;
import sistemaDelivery.modelo.Pedido;

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.function.Consumer;

/**
 * @author jvbor
 */
public class HandlerFinalizarItemPedido extends HandlerBotDelivery {

    private Categoria c;

    public HandlerFinalizarItemPedido(Categoria c, ChatBot chat) {
        super(chat);
        this.c = c.getRootCategoria();
    }

    @Override
    protected boolean runFirstTime(Message m) {
        MessageBuilder builder = new MessageBuilder();
        builder.textNewLine("O que você quer fazer agora?");
        Consumer<String> consumer = new Consumer<>() {
            @Override
            public void accept(String s) {
                Pedido p = getChatBotDelivery().getPedidoAtual();
                p.addItemPedido(getChatBotDelivery().getLastPedido());
            }
        };
        builder.textNewLine(addOpcaoMenu(new HandlerRepetirUltimoItemPedido(chat), consumer, "Pedir mais um(a) " + getChatBotDelivery().getLastPedido().getProduto().getNome() + " igual", "Os adicionais escolhidos serão iguais").toString());
        builder.textNewLine(addOpcaoMenu(new HandlerMenuCategoria(c, chat), consumer, "Pedir mais " + c.getNomeCategoria(), "", c.getNomeCategoria()).toString());
        Calendar dataAtual = Calendar.getInstance(getChatBotDelivery().getEstabelecimento().getTimeZoneObject());
        int diaSemana = dataAtual.get(Calendar.DAY_OF_WEEK) - 1;
        LocalTime horaAtual = getChatBotDelivery().getEstabelecimento().getHoraAtual();
        try {
            for (Categoria c : ControleCategorias.getInstance().getCategoriasEstabelecimento(getChatBotDelivery().getEstabelecimento())) {
                if (c.equals(this.c) || !c.isVisivel() || (c.getProdutos().isEmpty() && c.getCategoriasFilhas().isEmpty())) {
                    continue;
                }
                if (c.getRestricaoVisibilidade() != null) {
                    if (c.getRestricaoVisibilidade().isRestricaoDia()) {
                        if (!c.getRestricaoVisibilidade().getDiasSemana()[diaSemana]) {
                            continue;
                        }
                    }
                    if (c.getRestricaoVisibilidade().isRestricaoHorario()) {
                        if (!(horaAtual.isAfter(c.getRestricaoVisibilidade().getHorarioDe()) && horaAtual.isBefore(c.getRestricaoVisibilidade().getHorarioAte()))) {
                            continue;
                        }
                    }
                }
                builder.textNewLine(addOpcaoMenu(new HandlerMenuCategoria(c, chat), consumer, "Pedir " + c.getNomeCategoria(), "", c.getNomeCategoria()).toString());
            }
        } catch (SQLException e) {
            getChatBotDelivery().getChat().getDriver().onError(e);
        }
        builder.textNewLine(addOpcaoMenu(new HandlerCancelar(chat), null, "Cancelar Pedido ❌", "", "cancelar", "❌").toString());
        builder.textNewLine(addOpcaoMenu(new HandlerVerificaPedidoCorreto(chat), consumer, "Concluir Pedido ✅", "", "concluir", "✅").toString());
        chat.getChat().markComposing(1500).join();
        chat.getChat().sendMessage(builder.build()).join();
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        getChatBotDelivery().setHandlerVoltar(null);
        return processarOpcoesMenu(m);
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
