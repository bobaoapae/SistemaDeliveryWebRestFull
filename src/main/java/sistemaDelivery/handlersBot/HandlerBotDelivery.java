/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import handlersBot.HandlerBot;
import modelo.ChatBot;
import modelo.Message;
import modelo.MessageBuilder;
import modelo.exceptions.CantReply;
import sistemaDelivery.modelo.ChatBotDelivery;
import utils.Utilitarios;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author jvbor
 */
public abstract class HandlerBotDelivery extends HandlerBot {

    protected ArrayList<OpcaoMenu> codigosMenu;
    protected DecimalFormat moneyFormat = new DecimalFormat("###,###,###.00");
    private int qtdObs;
    private final char[] numerosElevados = {'⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹'};

    public HandlerBotDelivery(ChatBot chat) {
        super(chat);
        codigosMenu = new ArrayList<>();
    }

    private String gerarNumeroElevado() {
        String numeroAtual = String.valueOf(qtdObs++);
        if (numeroAtual.equals("0")) {
            return "";
        }
        String numeroElevado = "";
        for (char n : numeroAtual.toCharArray()) {
            numeroElevado += numerosElevados[Integer.parseInt(Character.toString(n))];
        }
        return numeroElevado;
    }

    protected void resetarObs() {
        qtdObs = 0;
    }

    protected String gerarObs(String observacao) {
        return "*Obs" + gerarNumeroElevado() + "*: " + observacao;
    }

    protected void resetarOpcoes() {
        codigosMenu.clear();
    }

    protected OpcaoMenu addOpcaoNao(HandlerBotDelivery handlerBotDelivery, Consumer<String> executar) {
        return addOpcaoMenu(handlerBotDelivery, executar, "Não", "", "não", "👎", "👎🏻", "👎🏼", "👎🏽", "👎🏾", "👎🏿");
    }

    protected OpcaoMenu addOpcaoSim(HandlerBotDelivery handlerBotDelivery, Consumer<String> executar) {
        return addOpcaoMenu(handlerBotDelivery, executar, "Sim", "", "sim", "👍", "👍🏻", "👍🏼", "👍🏽", "👍🏾", "👍🏿", "👌", "👌🏻", "👌🏼", "👌🏽", "👌🏾", "👌🏿", "🤙", "🤙🏻", "🤙🏼", "🤙🏽", "🤙🏾", "🤙🏿");
    }

    protected OpcaoMenu addOpcaoMenu(HandlerBotDelivery handler, Consumer<String> executar, String titulo, String subTitulo, String... keywords) {
        OpcaoMenu op = new OpcaoMenu(handler, executar, titulo, subTitulo, keywords);
        codigosMenu.add(op);
        return op;
    }

    protected String gerarTextoOpcoes() {
        MessageBuilder messageBuilder = new MessageBuilder();
        for (OpcaoMenu opcaoMenu : codigosMenu) {
            messageBuilder.textNewLine(opcaoMenu.toString());
        }
        return messageBuilder.build();
    }

    protected final boolean processarOpcoesMenu(Message msg) {
        String textoMsg = msg.getContent().trim();
        String textoSoNumero = Utilitarios.retornarApenasNumeros(textoMsg);
        List<OpcaoMenu> opcoesEncontradas;
        if (textoSoNumero.isEmpty()) {
            List<String> keywordsDisponiveis = new ArrayList<>();
            codigosMenu.stream().map(opcaoMenu -> opcaoMenu.keywords).collect(Collectors.toList()).forEach(keywordsDisponiveis::addAll);
            String textoMsgCorridigo = Utilitarios.corrigirStringComBaseEmListaDeStringsValidas(keywordsDisponiveis, textoMsg);
            opcoesEncontradas = codigosMenu.stream().filter(opcaoMenu -> opcaoMenu.verificarKeyword(textoMsgCorridigo)).collect(Collectors.toList());
        } else {
            opcoesEncontradas = codigosMenu.stream().filter(opcaoMenu -> codigosMenu.indexOf(opcaoMenu) + 1 == Integer.parseInt(textoSoNumero)).collect(Collectors.toList());
        }
        if (opcoesEncontradas.size() >= 1) {
            if (opcoesEncontradas.size() == 1) {
                return opcoesEncontradas.get(0).executar(textoMsg);
            } else {
                MessageBuilder messageBuilder = new MessageBuilder();
                messageBuilder.textNewLine("Foi encontrada mais de uma opção para a sua escolha, qual é o número da opção correta?");
                for (OpcaoMenu op : opcoesEncontradas) {
                    messageBuilder.textNewLine(op.toString());
                }
                try {
                    msg.replyMessage(messageBuilder.build());
                } catch (CantReply cantReply) {
                    chat.getChat().getDriver().onError(cantReply);
                    chat.getChat().sendMessage(messageBuilder.build());
                }
                return true;
            }
        }
        return false;
    }

    public ChatBotDelivery getChatBotDelivery() {
        return (ChatBotDelivery) chat;
    }

    public abstract boolean notificaPedidosFechados();

    public class OpcaoMenu {
        private Consumer<String> executar;
        private HandlerBotDelivery handlerBotDelivery;
        private String titulo;
        private String subTitulo;
        private List<String> keywords;

        public OpcaoMenu(HandlerBotDelivery handlerBotDelivery, Consumer<String> executar, String titulo, String subTitulo, String[] keywords) {
            this.handlerBotDelivery = handlerBotDelivery;
            this.titulo = titulo;
            this.subTitulo = subTitulo;
            this.keywords = Arrays.asList(keywords);
            this.executar = executar;
        }

        @Override
        public String toString() {
            int index = codigosMenu.indexOf(this) + 1;
            MessageBuilder msg = new MessageBuilder();
            msg.textNewLine("*" + index + "* - " + titulo);
            if (subTitulo != null && !subTitulo.isEmpty()) {
                msg.textNewLine("       _" + subTitulo + "_");
            }
            return msg.build();
        }

        protected boolean verificarKeyword(String msg) {
            for (String keyword : keywords) {
                return keyword.trim().equalsIgnoreCase(msg.trim());
            }
            return false;
        }

        protected boolean executar(String msg) {
            if (executar != null) {
                executar.accept(msg);
            }
            if (handlerBotDelivery != null) {
                chat.setHandler(handlerBotDelivery, true);
            }
            return true;
        }

        public HandlerBotDelivery getHandlerBotDelivery() {
            return handlerBotDelivery;
        }
    }

}
