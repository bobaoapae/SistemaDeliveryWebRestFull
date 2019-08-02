/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.AdicionalProduto;
import sistemaDelivery.modelo.GrupoAdicional;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        adicionaisDisponiveis.clear();
        adicionaisEscolhidos.clear();
        synchronized (grupoAtual.getAdicionais()) {
            if (!grupoAtual.getAdicionais().isEmpty() && grupoAtual.getAdicionais().stream().anyMatch(AdicionalProduto::isAtivo)) {
                chat.getChat().markComposing(3000);
                if (grupoAtual.getDescricaoGrupo().isEmpty()) {
                    if (grupoAtual.getQtdMax() > 1 || grupoAtual.getQtdMax() == 0) {
                        chat.getChat().sendMessage("Quais " + grupoAtual.getNomeGrupo() + " você quer?");
                    } else {
                        chat.getChat().sendMessage("Qual " + grupoAtual.getNomeGrupo() + " você quer?");
                    }
                } else {
                    chat.getChat().sendMessage(grupoAtual.getDescricaoGrupo());
                }
                String adicionais = "";
                for (AdicionalProduto ad : grupoAtual.getAdicionais()) {
                    if (!ad.isAtivo()) {
                        continue;
                    }
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
                chat.getChat().markComposing(4500);
                chat.getChat().sendMessage(adicionais);
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
                chat.getChat().markComposing(2000);
                if (grupoAtual.getQtdMax() > 1) {
                    chat.getChat().sendMessage(gerarObs("Você pode escolher no máximo " + grupoAtual.getQtdMax() + " " + grupoAtual.getNomeGrupo() + ". Envie o número da sua escolha, ou escolhas separadas por virgula. Ex: " + exemploEscolhas));
                } else if (grupoAtual.getQtdMax() == 1) {
                    chat.getChat().sendMessage(gerarObs("Envie o número da sua escolha."));
                } else {
                    chat.getChat().sendMessage(gerarObs("Envie o número da sua escolha, ou escolhas separadas por virgula. Ex: " + exemploEscolhas));
                }
                if (grupoAtual.getQtdMin() == 0) {
                    chat.getChat().sendMessage(gerarObs("Caso não deseje nada, basta enviar NÃO."));
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
            String[] adicionaisInformados = msg.getContent().replace(" ", "").replace(".", ",").replaceAll(",+", ",").split(",");
            int totalEscolhidos = 0;
            for (String adicionalAtual : adicionaisInformados) {
                if (grupoAtual.getQtdMax() > 0) {
                    if (totalEscolhidos == grupoAtual.getQtdMax()) {
                        break;
                    }
                }
                String soNumeros = Utils.retornarApenasNumeros(adicionalAtual);
                if (!soNumeros.isEmpty()) {
                    int escolha = Integer.parseInt(soNumeros) - 1;
                    if (escolha >= 0 && adicionaisDisponiveis.size() > escolha) {
                        adicionaisEscolhidos.add(adicionaisDisponiveis.get(escolha));
                        totalEscolhidos++;
                    } else {
                        adicionaisEscolhidos.clear();
                        return false;
                    }
                } else {
                    String possivelNomeAdicional = Utils.retornarApenasLetras(adicionalAtual);
                    boolean found = false;
                    String possivelNomeAdicionalCorrigido = Utils.corrigirStringComBaseEmListaDeStringsValidas(adicionaisDisponiveis.stream().map(adicionalProduto -> adicionalProduto.getNome()).collect(Collectors.toList()), possivelNomeAdicional);
                    for (AdicionalProduto ad : adicionaisDisponiveis) {
                        if (possivelNomeAdicionalCorrigido.equalsIgnoreCase(ad.getNome())) {
                            adicionaisEscolhidos.add(ad);
                            totalEscolhidos++;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        adicionaisEscolhidos.clear();
                        return false;
                    }
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
            chat.getChat().markComposing(2000);
            chat.getChat().sendMessage(grupoAtual.getNomeGrupo() + " do pedido: " + adicionais + ".");
            chat.getChat().markComposing(3000);
            chat.getChat().sendMessage("*_Obs: Caso a escolha esteja incorreta envie: VOLTAR_*");
            chat.getChat().markComposing(1500);
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
