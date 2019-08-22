/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.controle.ControleCategorias;
import sistemaDelivery.modelo.Categoria;

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.Calendar;

/**
 * @author jvbor
 */
public class HandlerMenuPrincipal extends HandlerBotDelivery {

    public HandlerMenuPrincipal(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().markComposing(2500);
        chat.getChat().sendMessage("Qual cardapio você gostaria de olhar?");
        Calendar dataAtual = Calendar.getInstance(getChatBotDelivery().getEstabelecimento().getTimeZoneObject());
        int diaSemana = dataAtual.get(Calendar.DAY_OF_WEEK) - 1;
        LocalTime horaAtual = getChatBotDelivery().getEstabelecimento().getHoraAtual();
        try {
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
                        if (!(horaAtual.isAfter(c.getRestricaoVisibilidade().getHorarioDe()) && horaAtual.isBefore(c.getRestricaoVisibilidade().getHorarioAte()))) {
                            continue;
                        }
                    }
                }
                addOpcaoMenu(new HandlerMenuCategoria(c, chat), null, c.getNomeCategoria(), "", c.getNomeCategoria());
            }
        } catch (SQLException e) {
            getChatBotDelivery().getChat().getDriver().onError(e);
        }
        if (getChatBotDelivery().getEstabelecimento().isReservas()) {
            addOpcaoMenu(new HandlerRealizarReserva(chat), null, "Realizar Reserva", "", "reserva");
        }
        if (!getChatBotDelivery().getEstabelecimento().getRodizios().isEmpty()) {
            addOpcaoMenu(new HandlerMenuRodizios(chat), null, "Ver Rodizios", "", "rodizios", "rodizio");
        }
        addOpcaoMenu(new HandlerAdeus(chat), null, "Cancelar Pedido ❌", "", "cancelar");
        if (getChatBotDelivery().getPedidoAtual() != null && getChatBotDelivery().getPedidoAtual().getProdutos().size() > 0) {
            addOpcaoMenu(new HandlerVerificaPedidoCorreto(chat), null, "Concluir Pedido ✅", "", "concluir");
        }
        chat.getChat().markComposing(3500);
        chat.getChat().sendMessage(gerarTextoOpcoes());
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        return processarOpcoesMenu(m);
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
