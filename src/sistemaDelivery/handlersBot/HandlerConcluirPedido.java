/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;
import sistemaDelivery.modelo.ChatBotDelivery;
import sistemaDelivery.modelo.Pedido;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author jvbor
 */
public class HandlerConcluirPedido extends HandlerBotDelivery {

    public HandlerConcluirPedido(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        Pedido p = ((ChatBotDelivery) chat).getPedidoAtual();
        p.setChat(chat);
        p.setNomeCliente(((ChatBotDelivery) chat).getNome());
        p.setCelular(((ChatBotDelivery) chat).getCliente().getTelefoneMovel());
        try {
            // ((ChatBotDelivery) chat).getCliente().realizaCompra(p);
            /*if (ControlePedidos.getInstance(Db4oGenerico.getInstance("banco")).salvar(p)) {
                chat.getChat().sendMessage("Tudo certo então!");
                if (!ControleCategorias.getInstance(Db4oGenerico.getInstance("banco")).pesquisarPorCodigo(-2).getProdutosCategoria().isEmpty()) {
                    chat.getChat().sendMessage("Já tenho todas as informações do seu pedido aqui, vou imprimir ele para o nosso Pizzaiolo e já te aviso.");
                } else {
                    chat.getChat().sendMessage("Já tenho todas as informações do seu pedido aqui, vou imprimir ele para a nossa àrea de produção e já te aviso.");
                }
                chat.getChat().sendMessage("😉");
                if (!ControleImpressao.getInstance().imprimir(((ChatBotDelivery) chat).getPedidoAtual())) {
                    try {
                        Chat c = driver.getFunctions().getChatByNumber("554491050665");
                        if (c != null) {
                            c.sendMessage("*" + Configuracao.getInstance().getNomeEstabelecimento() + ":* Falha ao Imprimir Pedido #" + p.getCod());
                        }
                        c = driver.getFunctions().getChatByNumber("55" + Utilitarios.plainText(Configuracao.getInstance().getNumeroAviso()));
                        if (c != null) {
                            c.sendMessage("*" + Configuracao.getInstance().getNomeEstabelecimento() + ":* Falha ao Imprimir Pedido #" + p.getCod());
                        }
                    } catch (Exception ex) {
                        driver.onError(ex);
                    }
                    new Thread() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(null, "Falha ao Imprimir o Pedido #" + p.getCod(), "Erro!", JOptionPane.ERROR_MESSAGE);
                        }
                    }.start();
                }
                chat.getChat().sendMessage("Pronto, " + p.getNomeCliente() + ". Seu pedido de numero #" + p.getCod() + " foi registrado e já está em produção\nCaso deseje realizar um novo pedido, basta me enviar uma mensagem");
                if (p.getHoraAgendamento() == null) {
                    if (!p.isEntrega()) {
                        chat.getChat().sendMessage("Em cerca de 10 à 15 minutos você já pode vir busca-lo.");
                    } else {
                        chat.getChat().sendMessage("Em cerca de 30 à 45 minutos ele sera entrege no endereço informado.");
                    }
                } else {
                    if (!p.isEntrega()) {
                        chat.getChat().sendMessage("Às " + p.getHoraAgendamento().format(DateTimeFormatter.ofPattern("HH:mm")) + " você já pode vir busca-lo.");
                    } else {
                        chat.getChat().sendMessage("Às " + p.getHoraAgendamento().format(DateTimeFormatter.ofPattern("HH:mm")) + " ele sera entregue no endereço informado.");
                    }
                }
                chat.setHandler(new HandlerPedidoConcluido(chat), true);
            } else {
                chat.setHandler(this, false);
                chat.getChat().sendMessage("Ouve um erro ao salvar seu pedido!");
                chat.getChat().sendMessage("Tente novamente em alguns minutos ou aguarde nosso Atendente ler suas mensagens.");
            }*/
        } catch (Exception ex) {
            chat.setHandler(this, false);
            chat.getChat().sendMessage("Ouve um erro ao salvar seu pedido!");
            chat.getChat().sendMessage("Tente novamente em alguns minutos ou aguarde nosso Atendente ler suas mensagens.");
            Logger.getLogger("LogDelivery").log(Level.SEVERE, null, ex);
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
