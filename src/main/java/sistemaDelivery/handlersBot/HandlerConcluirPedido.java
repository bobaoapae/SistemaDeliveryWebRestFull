/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import modelo.MessageBuilder;
import sistemaDelivery.controle.ControlePedidos;
import sistemaDelivery.modelo.Pedido;

import java.time.format.DateTimeFormatter;

/**
 * @author jvbor
 */
public class HandlerConcluirPedido extends HandlerBotDelivery {

    public HandlerConcluirPedido(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        Pedido p = getChatBotDelivery().getPedidoAtual();
        p.setNomeCliente(getChatBotDelivery().getNome());
        p.setCelular(getChatBotDelivery().getCliente().getTelefoneMovel());
        try {
            if (ControlePedidos.getInstance().salvarPedido(p)) {
                MessageBuilder builder = new MessageBuilder();
                builder.textNewLine("Pronto, " + p.getNomeCliente() + ". Seu pedido de numero #" + p.getCod() + " foi registrado e já está em produção.").newLine();
                if (p.getHoraAgendamento() == null) {
                    if (p.isEntrega()) {
                        builder.text("E não se preocupe, você receberá um aviso assim que seu pedido estiver saindo para a entrega, normalmente não demoramos mais do que ")
                                .textBold(p.getEstabelecimento().getTempoMedioEntrega() + " minutos");
                    } else {
                        builder.text("E não se preocupe, você receberá um aviso assim que seu pedido estiver pronto para a retirada, normalmente não demoramos mais do que ")
                                .textBoldNewLine(p.getEstabelecimento().getTempoMedioRetirada() + " minutos")
                                .newLine()
                                .text("Nosso endereço é o seguinte: ")
                                .textBold(p.getEstabelecimento().getEndereco());
                    }
                } else {
                    if (p.isEntrega()) {
                        builder.textNewLine("A partir das " + p.getHoraAgendamento().format(DateTimeFormatter.ofPattern("HH:mm")) + " ele sera entregue no endereço informado.")
                                .text("E não se preocupe, você recebera um aviso assim que seu pedido estiver saindo para a entrega.");
                    } else {
                        builder.text("A partir das " + p.getHoraAgendamento().format(DateTimeFormatter.ofPattern("HH:mm")) + " você recebera um aviso para buscar seu pedido.")
                                .newLine()
                                .text("Nosso endereço é o seguinte: ")
                                .textBold(p.getEstabelecimento().getEndereco());
                    }
                }
                chat.getChat().markComposing(5000);
                chat.getChat().sendMessage(builder.build());
                chat.setHandler(new HandlerPedidoConcluido(chat), true);
            } else {
                this.reset();
                chat.setHandler(this, false);
                chat.getChat().sendMessage("Ouve um erro ao salvar seu pedido!");
                chat.getChat().sendMessage("Tente novamente em alguns minutos ou aguarde nosso Atendente ler suas mensagens.");
            }
        } catch (Exception ex) {
            this.reset();
            chat.setHandler(this, false);
            chat.getChat().sendMessage("Ouve um erro ao salvar seu pedido!");
            chat.getChat().sendMessage("Tente novamente em alguns minutos ou aguarde nosso Atendente ler suas mensagens.");
            getChatBotDelivery().getChat().getDriver().onError(ex);
        }
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        return true;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
