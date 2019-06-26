/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import modelo.MessageBuilder;
import sistemaDelivery.modelo.AdicionalProduto;
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
    private boolean confirmandoAdicionais = false;
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
        adicionaisDisponiveis.clear();
        adicionaisEscolhidos.clear();
        synchronized (grupoAtual.getAdicionais()) {
            if (!grupoAtual.getAdicionais().isEmpty()) {
                if (grupoAtual.getDescricaoGrupo().isEmpty()) {
                    if (grupoAtual.getQtdMax() > 1) {
                        chat.getChat().sendMessage("Quais " + grupoAtual.getNomeGrupo() + " você quer?");
                    } else {
                        chat.getChat().sendMessage("Qual " + grupoAtual.getNomeGrupo() + " você quer?");
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
                chat.getChat().sendMessage(adicionais);
                if (grupoAtual.getQtdMax() > 1) {
                    chat.getChat().sendMessage("*_Obs¹: Você pode escolher no máximo " + grupoAtual.getQtdMax() + ". Envie o número da sua escolha, ou escolhas separadas por virgula. Ex: 1, 2, 3_*", 2000);
                } else if (grupoAtual.getQtdMax() == 1) {
                    chat.getChat().sendMessage("*_Obs¹: Envie o número da sua escolha._*", 2000);
                } else {
                    chat.getChat().sendMessage("*_Obs¹: Envie o número da sua escolha, ou escolhas separadas por virgula. Ex: 1, 2, 3_*", 2000);
                }
                if (grupoAtual.getQtdMin() == 0) {
                    chat.getChat().sendMessage("*_Obs²: Caso não deseje nada, basta enviar NÃO._*", 2000);
                }
            } else {
                chat.setHandler(nextHandler, true);
            }
        }
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        if (!confirmandoAdicionais) {
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
                confirmandoAdicionais = true;
                String adicionais = "";
                for (int x = 0; x < adicionaisEscolhidos.size(); x++) {
                    AdicionalProduto adicional = adicionaisEscolhidos.get(x);
                    adicionais += adicional.getNome();
                    if (x < adicionaisEscolhidos.size() - 1) {
                        adicionais += ",";
                    }
                }
                if (adicionais.endsWith(",")) {
                    adicionais = adicionais.substring(0, adicionais.lastIndexOf(","));
                }
                chat.getChat().sendMessage(grupoAtual.getNomeGrupo() + " do pedido: " + adicionais);
                chat.getChat().sendMessage("Sua escolha está correta? 🤞");
                chat.getChat().sendMessage("*_Obs: Envie somente o número da sua escolha_*");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                MessageBuilder builder = new MessageBuilder();
                builder.textNewLine("*1* - Sim").
                        textNewLine("*2* - Não");
                chat.getChat().sendMessage(builder.build());
                return true;
            } catch (NumberFormatException ex) {
                return false;
            } catch (Exception ex) {
                getChatBotDelivery().getChat().getDriver().onError(ex);
                return false;
            }
        } else {
            int escolha = Integer.parseInt(msg.getContent().trim());
            if (escolha == 1) {
                for (AdicionalProduto adicionalProduto : adicionaisEscolhidos) {
                    getChatBotDelivery().getLastPedido().addAdicional(adicionalProduto);
                }
                chat.setHandler(nextHandler, true);
            } else {
                confirmandoAdicionais = false;
                runFirstTime(null);
            }
            return true;
        }
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
