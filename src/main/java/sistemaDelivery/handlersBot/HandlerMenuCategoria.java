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

    public HandlerMenuCategoria(Categoria c, ChatBot chat) {
        super(chat);
        this.c = c;
    }

    @Override
    protected boolean runFirstTime(Message m) {
        MessageBuilder builder = new MessageBuilder();
        chat.getChat().markComposing(2000);
        chat.getChat().sendMessage("Seguem as opções de: " + c.getNomeCategoria() + ".");
        if (!c.isFazEntrega()) {
            chat.getChat().markComposing(2000);
            chat.getChat().sendMessage(gerarObs("Não é feita a entrega dos produtos abaixo"));
        } else {
            boolean temCategoriaPrecisa = false;
            boolean msg = false;
            List<Categoria> categoriasCompradas = new ArrayList<>();
            for (ItemPedido item2 : getChatBotDelivery().getPedidoAtual().getProdutos()) {
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
            if (!temCategoriaPrecisa || c.getRootCategoria().getQtdMinEntrega() > 1 && c.getRootCategoria().getQtdMinEntrega() > getChatBotDelivery().getPedidoAtual().getProdutos(c).size()) {
                msg = true;
            }
            if (msg) {
                chat.getChat().markComposing(3000);
                if (c.getRootCategoria().getQtdMinEntrega() > 1 && !c.getRootCategoria().isPrecisaPedirOutraCategoria()) {
                    chat.getChat().sendMessage(gerarObs("A entrega só e feita se você pedir no minimo " + c.getRootCategoria().getQtdMinEntrega() + " itens"));
                } else if (c.getRootCategoria().getQtdMinEntrega() > 1 && c.getRootCategoria().isPrecisaPedirOutraCategoria()) {
                    chat.getChat().sendMessage(gerarObs("A entrega só e feita se você pedir no minimo " + c.getRootCategoria().getQtdMinEntrega() + " itens ou pedir junto algum produto de outro cardapio"));
                } else if (c.getRootCategoria().isPrecisaPedirOutraCategoria()) {
                    chat.getChat().sendMessage(gerarObs("A entrega só e feita se você pedir junto algum produto de outro cardapio"));
                }
            }
        }
        gerarMenu(c, builder);
        builder.textNewLine("---------");
        builder.textNewLine(addOpcaoMenu(new HandlerMenuPrincipal(chat), null, "Voltar ao Menu Principal ↩", "", "voltar", "menu", "principal").toString());
        builder.textNewLine(addOpcaoMenu(new HandlerCancelar(chat), null, "Cancelar Pedido ❌", "", "cancelar").toString());
        builder.textNewLine(gerarObs("Escolha um item por vez"));
        chat.getChat().markComposing(5000);
        chat.getChat().sendMessage(builder.build());
        return true;
    }

    @Override
    protected boolean runSecondTime(Message m) {
        return processarOpcoesMenu(m);
    }

    private void gerarMenu(Categoria c, MessageBuilder builder) {
        Calendar dataAtual = Calendar.getInstance(getChatBotDelivery().getEstabelecimento().getTimeZoneObject());
        int diaSemana = dataAtual.get(Calendar.DAY_OF_WEEK) - 1;
        LocalTime horaAtual = getChatBotDelivery().getEstabelecimento().getHoraAtual();
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
                    if (!(horaAtual.isAfter(l.getRestricaoVisibilidade().getHorarioDe()) && horaAtual.isBefore(l.getRestricaoVisibilidade().getHorarioAte()))) {
                        continue;
                    }
                }
            }
            String titulo = "";
            String subTitulo = "";
            if (l.getValor() > 0) {
                titulo = "*" + l.getNome() + " R$" + moneyFormat.format(l.getValor()) + "*";
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
                    titulo = "*" + l.getNome() + "*\n    *Apartir de R$" + moneyFormat.format(valorMinimo) + "*";
                } else {
                    titulo = l.getNome();
                }
            }
            if (!l.getDescricao().trim().isEmpty()) {
                subTitulo = l.getDescricao();
            }
            builder.textNewLine(addOpcaoMenu(new HandlerInformesProdutoEscolhido(l, chat, new HandlerAdicionaisProduto(l, chat)), null, titulo, subTitulo, l.getNome()).toString());
        }
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
