/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.modelo;

import handlersBot.HandlerBot;
import modelo.Chat;
import modelo.ChatBot;
import modelo.Message;
import modelo.UserChat;
import sistemaDelivery.controle.ControleClientes;
import sistemaDelivery.handlersBot.HandlerBoasVindas;
import sistemaDelivery.handlersBot.HandlerBotDelivery;
import sistemaDelivery.handlersBot.HandlerChatExpirado;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Objects;

/**
 * @author jvbor
 */
public class ChatBotDelivery extends ChatBot {

    private DecimalFormat moneyFormat = new DecimalFormat("###,###,###.00");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private Pedido pedidoAtual;
    private ItemPedido lastPedido;
    private Cliente cliente;
    private Reserva reservaAtual;
    private Estabelecimento estabelecimento;

    public ChatBotDelivery(Chat chat, Estabelecimento estabelecimento, boolean autoPause) {
        super(chat, autoPause);
        this.estabelecimento = estabelecimento;
        Cliente cliente = ControleClientes.getInstance().getClienteChatId(chat.getId(), estabelecimento);
        if (cliente != null) {
            this.cliente = cliente;
            if (cliente.getTelefoneMovel().isEmpty()) {
                this.cliente.setTelefoneMovel(((UserChat) chat).getContact().getPhoneNumber());
            }
        } else {
            this.cliente = new Cliente(chat.getId(), estabelecimento);
            this.cliente.setTelefoneMovel(((UserChat) chat).getContact().getPhoneNumber());
            this.cliente.setNome(chat.getContact().getSafeName());
            ControleClientes.getInstance().salvarCliente(this.cliente);
            this.cliente = ControleClientes.getInstance().getClienteByUUID(this.cliente.getUuid());
        }
        this.handler = new HandlerBoasVindas(this);
    }

    public Estabelecimento getEstabelecimento() {
        return estabelecimento;
    }

    @Override
    public HandlerBot getHandler() {
        if (System.currentTimeMillis() - this.timeCheck >= 60000 * 30) {
            this.handler = new HandlerChatExpirado(this);
        }
        return super.getHandler();
    }

    public DecimalFormat getMoneyFormat() {
        return moneyFormat;
    }

    public void setMoneyFormat(DecimalFormat moneyFormat) {
        this.moneyFormat = moneyFormat;
    }

    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    public SimpleDateFormat getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(SimpleDateFormat timeFormat) {
        this.timeFormat = timeFormat;
    }

    public Pedido getPedidoAtual() {
        return pedidoAtual;
    }

    public void setPedidoAtual(Pedido pedidoAtual) {
        this.pedidoAtual = pedidoAtual;
    }

    public ItemPedido getLastPedido() {
        return lastPedido;
    }

    public void setLastPedido(ItemPedido lastPedido) {
        this.lastPedido = lastPedido;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Reserva getReservaAtual() {
        return reservaAtual;
    }

    public void setReservaAtual(Reserva reservaAtual) {
        this.reservaAtual = reservaAtual;
    }

    public String getNome() {
        if (cliente.isCadastroRealizado()) {
            return cliente.getNome();
        } else {
            return chat.getContact().getSafeName();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ChatBot other = (ChatBot) obj;
        if (!Objects.equals(this.chat, other.getChat())) {
            return false;
        }
        return true;
    }

    public void sendEncerramos() {
        setHandler(new HandlerBoasVindas(this), false);
        getChat().sendMessage(getNome() + ", sinto muito...Vejo que você estava no meio de um pedido, mas infelizmente encerramos os pedidos por hoje.");
        getChat().sendMessage("Aguardamos seu retorno.");
    }

    @Override
    public boolean sendRequestAjuda() {
        setQtdErroResposta(0);
        getChat().sendMessage("Parece que você precisa de ajuda, vou te transferir para nosso atendente.");
        getChat().sendMessage("Caso queira voltar para o atendimento automatico envie: *INICIAR*.");
        setPaused(true);
        return true;
    }

    @Override
    public void processNewMsg(Message m) {
        if (m.getChat().getContact().getId().equals("554491050665@c.us")) {
            if (m.getContent().toLowerCase().equals("/encerrar")) {

            }
        }
        getHandler().handle(m);
    }

    public void finalizar() {
        if (this.checkMsgs != null && !this.checkMsgs.isCancelled() && !this.checkMsgs.isDone()) {
            this.checkMsgs.cancel(true);
        }
        if (((HandlerBotDelivery) handler).notificaPedidosFechados()) {
            this.sendEncerramos();
        }
        this.executor.shutdown();
    }

}
