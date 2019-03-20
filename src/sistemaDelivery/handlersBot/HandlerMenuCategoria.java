/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import modelo.MessageBuilder;
import sistemaDelivery.modelo.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author jvbor
 */
public class HandlerMenuCategoria extends HandlerBotDelivery {

    private Categoria c;
    private ArrayList<HandlerBotDelivery> codigosMenu = new ArrayList<>();

    public HandlerMenuCategoria(Categoria c, ChatBot chat) {
        super(chat);
        this.c = c;
    }

    @Override
    protected boolean runFirstTime(Message m) {
        MessageBuilder builder = new MessageBuilder();
        chat.getChat().sendMessage("Segue as opções de: " + c.getNomeCategoria() + ".");
        chat.getChat().sendMessage("*_Obs¹: Envie somente o número da sua escolha_*");
        chat.getChat().sendMessage("*_Obs²: Escolha um item por vez_*", 2000);
        if (!c.isFazEntrega()) {
            chat.getChat().sendMessage("*_Obs³: Não é feita a entrega dos produtos à baixo_*", 3000);
        } else {
            boolean temCategoriaPrecisa = false;
            boolean msg = false;
            List<Categoria> categoriasCompradas = new ArrayList<>();
            for (ItemPedido item2 : ((ChatBotDelivery) chat).getPedidoAtual().getProdutos()) {
                if (!categoriasCompradas.contains(item2.getProduto().getCategoria().getRootCategoria())) {
                    categoriasCompradas.add(item2.getProduto().getCategoria().getRootCategoria());
                }
            }

            for (Categoria catPrecisa : c.getRootCategoria().getCategoriasNecessarias()) {
                if (categoriasCompradas.contains(catPrecisa)) {
                    temCategoriaPrecisa = true;
                    break;
                }
            }
            if (!temCategoriaPrecisa || c.getRootCategoria().getQtdMinEntrega() > 1 && c.getRootCategoria().getQtdMinEntrega() > ((ChatBotDelivery) chat).getPedidoAtual().getProdutos(c).size()) {
                msg = true;
            }
            if (c.getRootCategoria().getQtdMinEntrega() > 1 && !c.getRootCategoria().isPrecisaPedirOutraCategoria() && msg) {
                chat.getChat().sendMessage("*_Obs³: A entrega só e feita se você pedir no minimo " + c.getRootCategoria().getQtdMinEntrega() + " itens_*", 3000);
            } else if (c.getRootCategoria().getQtdMinEntrega() > 1 && c.getRootCategoria().isPrecisaPedirOutraCategoria() && msg) {
                chat.getChat().sendMessage("*_Obs³: A entrega só e feita se você pedir no minimo " + c.getRootCategoria().getQtdMinEntrega() + " itens ou pedir junto algum produto de outro cardapio_*", 3000);
            } else if (c.getRootCategoria().isPrecisaPedirOutraCategoria() && msg) {
                chat.getChat().sendMessage("*_Obs³: A entrega só e feita se você pedir junto algum produto de outro cardapio_*", 3000);
            }
        }
        gerarMenu(c, builder);
        builder.textNewLine("---------");
        codigosMenu.add(new HandlerMenuPrincipal(chat));
        builder.textNewLine("*" + (codigosMenu.size()) + "* - Voltar ao Menu Principal ↩️");
        codigosMenu.add(new HandlerAdeus(chat));
        builder.textNewLine("*" + (codigosMenu.size()) + "* - Cancelar Pedido ❌");
        builder.textNewLine("*_Obs¹: Envie somente o número da sua escolha_*");
        builder.textNewLine("*_Obs²: Escolha um item por vez_*");
        chat.getChat().sendMessage(builder.build());
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        try {
            int escolha = Integer.parseInt(m.getContent().trim()) - 1;
            if (escolha >= 0 && codigosMenu.size() > escolha) {
                chat.setHandler(codigosMenu.get(escolha), true);
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
    }

    private void gerarMenu(Categoria c, MessageBuilder builder) {
        Calendar dataAtual = Calendar.getInstance();
        int diaSemana = dataAtual.get(Calendar.DAY_OF_WEEK) - 1;
        LocalTime horaAtual = LocalTime.now();
        if (!c.equals(this.c)) {
            builder.text(".          *-" + c.getNomeCategoria() + "-*");
            builder.newLine();
            if (c.getRootCategoria().isFazEntrega()) {
                if (!c.isFazEntrega()) {
                    builder.text("(*Não é feita a entrega*)");
                }
            }
        }
        for (Categoria cF : c.getCategoriasFilhas()) {
            if (!cF.isVisivel()) {
                continue;
            }
            gerarMenu(cF, builder);
        }
        builder.newLine();
        for (Produto l : c.getProdutos()) {
            if (l.isOnlyLocal()) {
                continue;
            }
            if (!l.isVisivel()) {
                continue;
            }
            if (l.getRestricaoVisibilidade() != null) {
                if (l.getRestricaoVisibilidade().isRestricaoDia()) {
                    if (!l.getRestricaoVisibilidade().getDiasSemana()[diaSemana]) {
                        continue;
                    }
                }
                if (l.getRestricaoVisibilidade().isRestricaoHorario()) {
                    if (!(horaAtual.isAfter(l.getRestricaoVisibilidade().getHorarioDe().toLocalTime()) && horaAtual.isBefore(l.getRestricaoVisibilidade().getHorarioAte().toLocalTime()))) {
                        continue;
                    }
                }
            }
            codigosMenu.add(new HandlerVerificaEscolhaCorreta(l, chat, this, new HandlerAdicionaisProduto(l, chat)));
            if (l.getValor() > 0) {
                builder.textNewLine("*" + (codigosMenu.size()) + " - " + l.getNome() + " R$" + moneyFormat.format(l.getValor()) + "*");

            } else {
                double valorMinimo = 0;
                for (GrupoAdicional grupoAdicional : l.getAllGruposAdicionais()) {
                    if (grupoAdicional.getAdicionais().size() == 0) {
                        continue;
                    }
                    if (grupoAdicional.getQtdMin() > 0) {
                        double menorValor = grupoAdicional.getAdicionais().get(0).getValor();
                        for (AdicionalProduto ad : grupoAdicional.getAdicionais()) {
                            if (ad.getValor() < menorValor) {
                                menorValor = ad.getValor();
                            }
                        }
                        valorMinimo += menorValor;
                    }
                }
                if (valorMinimo > 0) {
                    builder.textNewLine("*" + (codigosMenu.size()) + " - " + l.getNome() + "*\n    *Apartir de R$" + moneyFormat.format(valorMinimo) + "*");
                } else {
                    builder.textNewLine("*" + (codigosMenu.size()) + " - " + l.getNome() + "*");
                }
            }
            if (!l.getDescricao().trim().isEmpty()) {
                builder.textNewLine("       _" + l.getDescricao() + "_");
            }
            builder.newLine();
        }
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
