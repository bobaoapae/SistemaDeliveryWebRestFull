/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import modelo.MessageBuilder;
import sistemaDelivery.modelo.ChatBotDelivery;
import sistemaDelivery.modelo.Pedido;

import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author jvbor
 */
public class HandlerBoasVindas extends HandlerBotDelivery {

    public HandlerBoasVindas(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        int horaAtual = getChatBotDelivery().getEstabelecimento().getHoraAtual().getHour();
        String msg = "";
        if (horaAtual >= 2 && horaAtual < 12) {
            msg = "Bom Dia";
        } else if (horaAtual >= 12 && horaAtual < 18) {
            msg = "Boa Tarde";
        } else {
            msg = "Boa Noite";
        }
        MessageBuilder builder = new MessageBuilder();
        builder.text(msg).text(" ").text(((ChatBotDelivery) chat).getNome()).text(".").newLine();
        builder.textNewLine("Eu sou o " + getChatBotDelivery().getEstabelecimento().getNomeBot() + ", atendende virtual da " + getChatBotDelivery().getEstabelecimento().getNomeEstabelecimento() + ", e irei te ajudar a completar seu pedido.").
                textNewLine("*_Lembre-se de ler as instruções com atenção_*");
        ((ChatBotDelivery) chat).setPedidoAtual(new Pedido(((ChatBotDelivery) chat).getCliente(), getChatBotDelivery().getEstabelecimento()));
        chat.getChat().sendMessage(builder.build());
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(HandlerBoasVindas.class.getName()).log(Level.SEVERE, null, ex);
            Thread.currentThread().interrupt();
        }
        if (!getChatBotDelivery().getEstabelecimento().isOpenPedidos()) {
            if (getChatBotDelivery().getEstabelecimento().nextOrCurrentHorarioAbertoOfDay() == null) {
                if (!getChatBotDelivery().getEstabelecimento().checkTemHorarioFuncionamentoHoje()) {
                    chat.getChat().sendMessage("_Obs: Não realizamos atendimentos hoje_", 3500);
                } else {
                    chat.getChat().sendMessage("_Obs: Já encerramos os atendimentos por hoje_", 3500);
                }
                chat.setHandler(new HandlerAdeus(chat), true);
            } else if (getChatBotDelivery().getEstabelecimento().isAgendamentoDePedidos()) {
                chat.getChat().sendMessage("_Obs: Não iniciamos nosso atendimento ainda, porém você pode deixar seu pedido agendado._", 3000);
                chat.setHandler(new HandlerMenuPrincipal(chat), true);
            } else if (getChatBotDelivery().getEstabelecimento().isReservas() && getChatBotDelivery().getEstabelecimento().isReservasComPedidosFechados()) {
                chat.getChat().sendMessage("_Obs: Não iniciamos nosso atendimento ainda, nosso atendimento iniciasse às " + getChatBotDelivery().getEstabelecimento().nextOrCurrentHorarioAbertoOfDay().getHoraAbrir().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + ", porém você já pode realizar sua reserva de mesa_", 3500);
                chat.setHandler(new HandlerDesejaFazerUmaReserva(chat), true);
            } else {
                chat.getChat().sendMessage("_Obs: Não iniciamos nosso atendimento ainda, nosso atendimento iniciasse às " + getChatBotDelivery().getEstabelecimento().nextOrCurrentHorarioAbertoOfDay().getHoraAbrir().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + "._", 3500);
                chat.setHandler(new HandlerAdeus(chat), true);
            }
        } else {
            boolean possuiEntrega = getChatBotDelivery().getEstabelecimento().possuiEntrega();
            if (possuiEntrega) {
                chat.getChat().sendMessage("Informo que nosso prazo médio para entrega é de " + getChatBotDelivery().getEstabelecimento().getTempoMedioEntrega() + " à " + (getChatBotDelivery().getEstabelecimento().getTempoMedioEntrega() + 15) + " minutos. Já para retirada cerca de " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada()) + " à " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada() + 5) + " minutos.", 2000);
            } else {
                chat.getChat().sendMessage("Informo que nosso prazo médio para retirada é de " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada()) + " à " + (getChatBotDelivery().getEstabelecimento().getTempoMedioRetirada() + 5) + " minutos.", 2000);
            }
            chat.setHandler(new HandlerMenuPrincipal(chat), true);
        }
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        return runFirstTime(m);
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
