/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.ChatBotDelivery;
import sistemaDelivery.modelo.GrupoAdicional;
import sistemaDelivery.modelo.Produto;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jvbor
 */
public class HandlerAdicionaisProduto extends HandlerBotDelivery {

    private Produto p;
    private List<GrupoAdicional> gruposDisponiveis;
    private List<GrupoAdicional> gruposJaForam;

    public HandlerAdicionaisProduto(Produto p, ChatBot chat) {
        super(chat);
        this.p = p;
        gruposDisponiveis = p.getAllGruposAdicionais();
        gruposJaForam = new ArrayList<>();
    }

    @Override
    protected boolean runFirstTime(Message m) {
        if (!gruposDisponiveis.isEmpty()) {
            for (GrupoAdicional grupo : gruposDisponiveis) {
                if (gruposJaForam.contains(grupo)) {
                    continue;
                }
                gruposJaForam.add(grupo);
                synchronized (grupo.getAdicionais()) {
                    if (grupo.getAdicionais().size() <= grupo.getQtdMin()) {
                        ((ChatBotDelivery) chat).getLastPedido().getAdicionais().addAll(grupo.getAdicionais());
                        continue;
                    }
                }
                chat.setHandler(new HandlerEscolhaAdicionalDoGrupo(grupo, this, chat), true);
                return true;
            }
        }
        chat.setHandler(new HandlerComentarioPedido(chat, new HandlerDesejaMaisCategoria(p.getCategoria(), chat)), true);
        return true;
    }

    public void pedirGrupoNovamente(GrupoAdicional grupoAdicional) {
        List<GrupoAdicional> grupoAdicionals = new ArrayList<>();
        int index = gruposJaForam.indexOf(grupoAdicional);
        for (int x = index; x < gruposJaForam.size(); x++) {
            grupoAdicionals.add(gruposJaForam.get(x));
        }
        gruposJaForam.removeAll(grupoAdicionals);
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        return runFirstTime(msg);
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
