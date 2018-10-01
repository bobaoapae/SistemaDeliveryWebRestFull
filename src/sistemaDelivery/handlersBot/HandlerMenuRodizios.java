/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;

/**
 * @author jvbor
 */
public class HandlerMenuRodizios extends HandlerBotDelivery {

    private int lastCodeRodizio;

    public HandlerMenuRodizios(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
       /* MessageBuilder builder = new MessageBuilder();
        builder.textNewLine("Sobre qual rodizio você gostaria de obter informações?");
        builder.textNewLine("*_Obs: Envie somente o número da sua escolha_*");
        lastCodeRodizio = 0;
        for (Rodizio r : ControleRodizios.getInstance(Db4oGenerico.getInstance("banco")).carregarTodos()) {
            builder.textNewLine(r.getCod() + " - *" + r.getNome() + "* - R$" + new DecimalFormat("###,###,###.00").format(r.getValor()));
            lastCodeRodizio = r.getCod();
        }
        builder.textNewLine(lastCodeRodizio + 1 + " - Voltar ao Menu Principal ↩️");
        chat.getChat().sendMessage(builder.build());*/
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        String escolha = m.getContent().trim();
        if (escolha.equals(lastCodeRodizio + 1 + "") || escolha.toLowerCase().equals("volta")) {
            chat.setHandler(new HandlerMenuPrincipal(chat), true);
            return true;
        }
        /*
        try {
            int idRodizioInt = Integer.parseInt(escolha);
            Rodizio r = ControleRodizios.getInstance(Db4oGenerico.getInstance("banco")).pesquisarPorCodigo(idRodizioInt);
            if (r == null) {
                return false;
            } else {
                MessageBuilder builder = new MessageBuilder();
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
                chat.getChat().sendMessage(builder.build());
                chat.setHandler(new HandlerDesejaFazerUmaReserva(chat), true);
                return true;
            }
        } catch (Exception ex) {

            return false;
        }*/
        return false;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
