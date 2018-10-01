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

/**
 * @author jvbor
 */
public class HandlerDesejaFazerUmaReserva extends HandlerBotDelivery {

    public HandlerDesejaFazerUmaReserva(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        MessageBuilder builder = new MessageBuilder();
        /*ArrayList<Rodizio> rodizios = ControleRodizios.getInstance(Db4oGenerico.getInstance("banco")).rodiziosDoDia();
        if (rodizios.size() > 0) {
            chat.getChat().sendMessage("Antes que eu me esqueça, hoje é dia de rodízio aqui na " + Configuracao.getInstance().getNomeEstabelecimento());
            builder = new MessageBuilder();
            builder.textNewLine("Teremos os seguintes rodízios:");
            for (Rodizio r : rodizios) {
                String diasSemana = "";
                if (r.isDomingo()) {
                    diasSemana += "Domingo,";
                }
                if (r.isSegunda()) {
                    diasSemana += "Segunda,";
                }
                if (r.isTerca()) {
                    diasSemana += "Terca,";
                }
                if (r.isQuarta()) {
                    diasSemana += "Quarta,";
                }
                if (r.isQuinta()) {
                    diasSemana += "Quinta,";
                }
                if (r.isSexta()) {
                    diasSemana += "Sexta,";
                }
                if (r.isSabado()) {
                    diasSemana += "Sabado,";
                }
                diasSemana = diasSemana.substring(0, diasSemana.lastIndexOf(","));
                builder.textNewLine("*" + r.getNome() + "*");
                builder.textNewLine(r.getDescricao());
                builder.textNewLine("Tudo isso por apenas, R$" + new DecimalFormat("###,###,###.00").format(r.getValor()));
                builder.textNewLine("Dias da Semana: " + diasSemana);
                builder.textNewLine("Horario: " + r.getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")));
                builder.textNewLine("");
                builder.textNewLine("");
            }
            chat.getChat().sendMessage(builder.build());
        }*/
        chat.getChat().sendMessage("Você deseja realizar uma reserva agora?");
        chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*");
        chat.getChat().sendMessage("1 - Sim");
        chat.getChat().sendMessage("2 - Não");
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        if (msg.getContent().trim().equals("1") || msg.getContent().toLowerCase().trim().equals("sim") || msg.getContent().toLowerCase().trim().equals("s")) {
            ((ChatBotDelivery) chat).setPedidoAtual(new Pedido(getChatBotDelivery().getEstabelecimento()));
            chat.setHandler(new HandlerRealizarReserva(chat), true);
        } else if (msg.getContent().trim().equals("2") || msg.getContent().toLowerCase().trim().equals("não") || msg.getContent().toLowerCase().trim().equals("nao") || msg.getContent().toLowerCase().trim().equals("n")) {
            chat.setHandler(new HandlerAdeus(chat), true);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
