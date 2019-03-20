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
import sistemaDelivery.modelo.ChatBotDelivery;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * @author jvbor
 */
public class HandlerMenuPrincipal extends HandlerBotDelivery {

    private ArrayList<HandlerBotDelivery> codigosMenu = new ArrayList<>();

    public HandlerMenuPrincipal(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        codigosMenu.clear();
        MessageBuilder builder = new MessageBuilder();
        chat.getChat().sendMessage("Qual cardapio você gostaria de olhar?", 2000);
        builder.textNewLine("*_Obs: Envie somente o número da sua escolha_*");
        Calendar dataAtual = Calendar.getInstance();
        int diaSemana = dataAtual.get(Calendar.DAY_OF_WEEK) - 1;
        LocalTime horaAtual = LocalTime.now();
        for (Categoria c : ControleCategorias.getInstance().getCategoriasEstabelecimento(getChatBotDelivery().getEstabelecimento())) {
            if (c.getProdutos().isEmpty() && c.getCategoriasFilhas().isEmpty()) {
                continue;
            }
            if (!c.isAtivo()) {
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
            codigosMenu.add(new HandlerMenuCategoria(c, chat));
            builder.textNewLine("*" + (codigosMenu.size()) + "* - " + c.getNomeCategoria());
        }
        if (getChatBotDelivery().getEstabelecimento().isReservas()) {
            codigosMenu.add(new HandlerRealizarReserva(chat));
            builder.textNewLine("*" + (codigosMenu.size()) + "* - Realizar Reserva");
        }
        if (!((ChatBotDelivery) chat).getEstabelecimento().getRodizios().isEmpty()) {
            codigosMenu.add(new HandlerMenuRodizios(chat));
            builder.textNewLine("*" + (codigosMenu.size()) + "* - Ver Rodizios");
        }
        codigosMenu.add(new HandlerAdeus(chat));
        builder.textNewLine("*" + (codigosMenu.size()) + "* - Cancelar Pedido ❌");
        if (((ChatBotDelivery) chat).getPedidoAtual() != null && ((ChatBotDelivery) chat).getPedidoAtual().getProdutos().size() > 0) {
            codigosMenu.add(new HandlerVerificaPedidoCorreto(chat));
            builder.textNewLine("*" + (codigosMenu.size()) + "* - Concluir Pedido ✅");
        }
        chat.getChat().sendMessage(builder.build());
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        try {
            int escolha = Integer.parseInt(m.getContent().trim()) - 1;
            if (escolha >= 0 && codigosMenu.size() > escolha) {
                chat.setHandler(codigosMenu.get(escolha), true);
            } else {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
