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

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.Calendar;

/**
 * @author jvbor
 */
public class HandlerComentarioPedido extends HandlerBotDelivery {

    private HandlerBotDelivery nextHandler;

    public HandlerComentarioPedido(ChatBot chat, HandlerBotDelivery nextHandler) {
        super(chat);
        this.nextHandler = nextHandler;
    }

    @Override
    protected boolean runFirstTime(Message m) {
        if (getChatBotDelivery().getLastPedido().getProduto().getCategoria().getExemplosComentarioPedido().isEmpty()) {
            getChatBotDelivery().getLastPedido().setComentario("");
            chat.setHandler(nextHandler, true);
            return true;
        }
        chat.getChat().sendMessage("Você deseja modificar algo em seu pedido?");
        MessageBuilder builder = new MessageBuilder();
        builder.textNewLine(":");
        if (getChatBotDelivery().getLastPedido().getProduto().getCategoria().getExemplosComentarioPedido() != null && !getChatBotDelivery().getLastPedido().getProduto().getCategoria().getExemplosComentarioPedido().isEmpty()) {
            chat.getChat().sendMessage("Por exemplo: " + getChatBotDelivery().getLastPedido().getProduto().getCategoria().getExemplosComentarioPedido() + "... etc");
        }
        chat.getChat().sendMessage("Basta escrever e me enviar, o que você escrever sera repassado para a àrea de produção", 300);
        chat.getChat().sendMessage("*_Obs¹: Caso não queira modificar nada, basta enviar NÃO_*");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        Calendar dataAtual = Calendar.getInstance(getChatBotDelivery().getEstabelecimento().getTimeZoneObject());
        int diaSemana = dataAtual.get(Calendar.DAY_OF_WEEK) - 1;
        LocalTime horaAtual = getChatBotDelivery().getEstabelecimento().getHoraAtual();
        String categoriasDisponiveis = "";
        try {
            for (Categoria c : ControleCategorias.getInstance().getCategoriasEstabelecimento(getChatBotDelivery().getEstabelecimento())) {
                if (c.getProdutos().isEmpty() && c.getCategoriasFilhas().isEmpty()) {
                    continue;
                }
                if (!c.isVisivel()) {
                    continue;
                }
                if (c.getRestricaoVisibilidade() != null) {
                    if (c.getRestricaoVisibilidade().isRestricaoDia()) {
                        if (!c.getRestricaoVisibilidade().getDiasSemana()[diaSemana]) {
                            continue;
                        }
                    }
                    if (c.getRestricaoVisibilidade().isRestricaoHorario()) {
                        if (!(horaAtual.isAfter(c.getRestricaoVisibilidade().getHorarioDe().toLocalTime()) && horaAtual.isBefore(c.getRestricaoVisibilidade().getHorarioAte().toLocalTime()))) {
                            continue;
                        }
                    }
                }
                categoriasDisponiveis += c.getNomeCategoria() + ", ";
            }
        } catch (SQLException e) {
            getChatBotDelivery().getChat().getDriver().onError(e);
        }
        categoriasDisponiveis = categoriasDisponiveis.trim().substring(0, categoriasDisponiveis.lastIndexOf(","));
        if (categoriasDisponiveis.contains(", ")) {
            categoriasDisponiveis = categoriasDisponiveis.substring(0, categoriasDisponiveis.lastIndexOf(",")) + " ou" + categoriasDisponiveis.substring(categoriasDisponiveis.lastIndexOf(",") + 1);
        }
        chat.getChat().sendMessage("*_Obs²: Não use esse campo para pedir " + categoriasDisponiveis + " aguarde as próximas opções para isso_*");
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        getChatBotDelivery().setHandlerVoltar(null);
        if (msg.getContent().toLowerCase().trim().equals("não") || msg.getContent().toLowerCase().trim().equals("nao") || msg.getContent().toLowerCase().trim().equals("n")) {
            getChatBotDelivery().getLastPedido().setComentario("");
        } else {
            getChatBotDelivery().getLastPedido().setComentario(msg.getContent().trim());
            chat.getChat().sendMessage("Perfeito, já anotei aqui o que você me disse ✌️😉");
        }
        chat.setHandler(nextHandler, true);
        return true;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
