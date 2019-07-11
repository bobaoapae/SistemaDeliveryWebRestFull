/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.AdicionalProduto;
import sistemaDelivery.modelo.Categoria;
import sistemaDelivery.modelo.GrupoAdicional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jvbor
 */
public class HandlerEscolhaAdicionalDoGrupo extends HandlerBotDelivery {

    private List<AdicionalProduto> adicionaisDisponiveis;
    private GrupoAdicional grupoAtual;
    private HandlerBotDelivery nextHandler;
    private List<AdicionalProduto> adicionaisEscolhidos;

    public HandlerEscolhaAdicionalDoGrupo(GrupoAdicional grupoAtual, HandlerBotDelivery nextHandler, ChatBot chat) {
        super(chat);
        this.grupoAtual = grupoAtual;
        this.nextHandler = nextHandler;
        nextHandler.reset();
        this.adicionaisEscolhidos = new ArrayList<>();
        this.adicionaisDisponiveis = new ArrayList<>();
    }

    @Override
    protected boolean runFirstTime(Message m) {
        Categoria categoria = grupoAtual.getCategoria();
        if (categoria == null) {
            categoria = grupoAtual.getProduto().getCategoria();
        }
        adicionaisDisponiveis.clear();
        adicionaisEscolhidos.clear();
        synchronized (grupoAtual.getAdicionais()) {
            if (!grupoAtual.getAdicionais().isEmpty()) {
                if (grupoAtual.getDescricaoGrupo().isEmpty()) {
                    if (grupoAtual.getQtdMax() > 2 || grupoAtual.getQtdMax() == 0) {
                        chat.getChat().sendMessage("Quais " + grupoAtual.getNomeGrupo() + " você quer?", 2000);
                    } else {
                        chat.getChat().sendMessage("Qual " + grupoAtual.getNomeGrupo() + " você quer?", 2000);
                    }
                } else {
                    chat.getChat().sendMessage(grupoAtual.getDescricaoGrupo());
                }
                String adicionais = "";
                for (AdicionalProduto ad : grupoAtual.getAdicionais()) {
                    adicionaisDisponiveis.add(ad);
                    if (ad.getValor() > 0) {
                        adicionais += "*" + adicionaisDisponiveis.size() + "* - " + ad.getNome() + " - R$ " + moneyFormat.format(ad.getValor()) + "\n";
                    } else {
                        adicionais += "*" + adicionaisDisponiveis.size() + "* - " + ad.getNome() + "\n";
                    }
                    if (!ad.getDescricao().isEmpty()) {
                        adicionais += "_" + ad.getDescricao() + "_\n";
                    }
                    adicionais += "\n";
                }
                chat.getChat().sendMessage(adicionais, 2000);
                String exemploEscolhas = "";
                if (grupoAtual.getQtdMax() == 0) {
                    exemploEscolhas = "1, 2";
                } else {
                    for (int x = 1; x <= grupoAtual.getQtdMax(); x++) {
                        exemploEscolhas += x + ", ";
                    }
                }
                if (exemploEscolhas.endsWith(", ")) {
                    exemploEscolhas = exemploEscolhas.substring(0, exemploEscolhas.lastIndexOf(", "));
                }
                if (grupoAtual.getQtdMax() > 1) {
                    chat.getChat().sendMessage("*_Obs¹: Você pode escolher no máximo " + grupoAtual.getQtdMax() + " " + grupoAtual.getNomeGrupo() + ". Envie o número da sua escolha, ou escolhas separadas por virgula. Ex: " + exemploEscolhas + "_*");
                } else if (grupoAtual.getQtdMax() == 1) {
                    chat.getChat().sendMessage("*_Obs¹: Envie o número da sua escolha._*");
                } else {
                    chat.getChat().sendMessage("*_Obs¹: Envie o número da sua escolha, ou escolhas separadas por virgula. Ex: " + exemploEscolhas + "_*");
                }
                if (grupoAtual.getQtdMin() == 0) {
                    chat.getChat().sendMessage("*_Obs²: Caso não deseje nada, basta enviar NÃO._*");
                }
            } else {
                chat.setHandler(nextHandler, true);
            }
        }
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        try {
            if (grupoAtual.getQtdMin() == 0 && (msg.getContent().toLowerCase().trim().contains("não") || msg.getContent().toLowerCase().trim().contains("nao") || msg.getContent().toLowerCase().trim().equals("n"))) {
                chat.setHandler(nextHandler, true);
                return true;
            }
            String[] idAdicional = msg.getContent().replaceAll(" ", "").split(",");
            int totalEscolhidos = 0;
            for (String idAtual : idAdicional) {
                if (grupoAtual.getQtdMax() > 0) {
                    if (totalEscolhidos == grupoAtual.getQtdMax()) {
                        break;
                    }
                }
                int escolha = Integer.parseInt(idAtual) - 1;
                if (escolha >= 0 && adicionaisDisponiveis.size() > escolha) {
                    adicionaisEscolhidos.add(adicionaisDisponiveis.get(escolha));
                    totalEscolhidos++;
                } else {
                    adicionaisEscolhidos.clear();
                    return false;
                }
            }
            String adicionais = "";
            for (int x = 0; x < adicionaisEscolhidos.size(); x++) {
                AdicionalProduto adicional = adicionaisEscolhidos.get(x);
                getChatBotDelivery().getLastPedido().addAdicional(adicional);
                adicionais += adicional.getNome();
                if (x < adicionaisEscolhidos.size() - 1) {
                    adicionais += ", ";
                }
            }
            chat.getChat().sendMessage(grupoAtual.getNomeGrupo() + " do pedido: " + adicionais + ".", 2000);
            chat.getChat().sendMessage("*_Obs¹: Caso a escolha esteja incorreta envie: VOLTAR_*", 500);
            getChatBotDelivery().setHandlerVoltar(new HandlerVoltar(nextHandler, new Runnable() {
                @Override
                public void run() {
                    ((HandlerAdicionaisProduto) nextHandler).pedirGrupoNovamente(grupoAtual);
                    getChatBotDelivery().getLastPedido().getAdicionais().removeAll(adicionaisEscolhidos);
                }
            }, true));
            chat.setHandler(nextHandler, true);
            return true;
        } catch (NumberFormatException ex) {
            adicionaisEscolhidos.clear();
            return false;
        } catch (Exception ex) {
            adicionaisEscolhidos.clear();
            getChatBotDelivery().getChat().getDriver().onError(ex);
            return false;
        }
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
