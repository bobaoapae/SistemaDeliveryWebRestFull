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
import restFul.controle.ControleSessions;
import sistemaDelivery.SistemaDelivery;
import sistemaDelivery.controle.ControleClientes;
import sistemaDelivery.handlersBot.*;
import utils.Utilitarios;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

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

    public ChatBotDelivery(Chat chat, Estabelecimento estabelecimento, boolean autoPause) throws SQLException {
        super(chat, autoPause);
        this.estabelecimento = estabelecimento;
        Cliente cliente = ControleClientes.getInstance().getClienteChatId(chat.getId(), estabelecimento);
        if (cliente != null) {
            this.cliente = cliente;
            if (cliente.getTelefoneMovel().isEmpty()) {
                this.cliente.setTelefoneMovel(chat.getContact().getPhoneNumber());
            }
            if (!cliente.isCadastroRealizado()) {
                this.cliente.setNome(chat.getContact().getSafeName());
            }
            ControleClientes.getInstance().salvarCliente(this.cliente);
        } else {
            this.cliente = new Cliente(chat.getId(), estabelecimento);
            this.cliente.setTelefoneMovel(chat.getContact().getPhoneNumber());
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
            return getChat().getContact().getSafeName();
        }
    }

    public void sendEncerramos() {
        setHandler(new HandlerBoasVindas(this), false);
        getChat().sendMessage(getNome() + ", sinto muito...Vejo que você estava no meio de um pedido, mas infelizmente encerramos os pedidos por hoje.");
        getChat().sendMessage("Aguardamos seu retorno.");
    }

    public void enviarMensageInformesIniciais() {
        if (!this.getEstabelecimento().isOpenPedidos()) {
            if (this.getEstabelecimento().nextOrCurrentHorarioAbertoOfDay() == null) {
                if (!this.getEstabelecimento().checkTemHorarioFuncionamentoHoje()) {
                    chat.sendMessage("_Obs: Não realizamos atendimentos hoje_", 3500);
                } else {
                    chat.sendMessage("_Obs: Já encerramos os atendimentos por hoje_", 3500);
                }
                this.setHandler(new HandlerAdeus(this), true);
            } else if (this.getEstabelecimento().isAgendamentoDePedidos()) {
                chat.sendMessage("_Obs: Não iniciamos nosso atendimento ainda, porém você pode deixar seu pedido agendado._", 3000);
                this.setHandler(new HandlerMenuPrincipal(this), true);
            } else if (this.getEstabelecimento().isReservas() && this.getEstabelecimento().isReservasComPedidosFechados()) {
                chat.sendMessage("_Obs: Não iniciamos nosso atendimento ainda, nosso atendimento iniciasse às " + this.getEstabelecimento().nextOrCurrentHorarioAbertoOfDay().getHoraAbrir().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + ", porém você já pode realizar sua reserva de mesa_", 3500);
                this.setHandler(new HandlerDesejaFazerUmaReserva(this), true);
            } else {
                chat.sendMessage("_Obs: Não iniciamos nosso atendimento ainda, nosso atendimento iniciasse às " + this.getEstabelecimento().nextOrCurrentHorarioAbertoOfDay().getHoraAbrir().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + "._", 3500);
                this.setHandler(new HandlerAdeus(this), true);
            }
        } else {
            boolean possuiEntrega = this.getEstabelecimento().possuiEntrega();
            if (possuiEntrega) {
                chat.sendMessage("Informo que nosso prazo médio para entrega é de " + this.getEstabelecimento().getTempoMedioEntrega() + " à " + (this.getEstabelecimento().getTempoMedioEntrega() + 15) + " minutos. Já para retirada cerca de " + (this.getEstabelecimento().getTempoMedioRetirada()) + " à " + (this.getEstabelecimento().getTempoMedioRetirada() + 5) + " minutos.", 2000);
            } else {
                chat.sendMessage("Informo que nosso prazo médio para retirada é de " + (this.getEstabelecimento().getTempoMedioRetirada()) + " à " + (this.getEstabelecimento().getTempoMedioRetirada() + 5) + " minutos.", 2000);
            }
            this.setHandler(new HandlerMenuPrincipal(this), true);
        }
    }

    @Override
    public boolean sendRequestAjuda() {
        if (getChat().getDriver().getFunctions().isBusiness()) {
            getChat().addLabel("Precisa de Ajuda", true);
        }
        setQtdErroResposta(0);
        getChat().sendMessage("Parece que você precisa de ajuda, vou te transferir para nosso atendente.");
        getChat().sendMessage("Caso queira voltar para o atendimento automatico envie: *INICIAR*.");
        setPaused(true);
        try {
            SistemaDelivery sistemaDelivery = ControleSessions.getInstance().getSessionForEstabelecimento(estabelecimento);
            if (sistemaDelivery != null && sistemaDelivery.getBroadcaster() != null) {
                sistemaDelivery.getBroadcaster().broadcast(sistemaDelivery.getSse().newEvent("pedido-ajuda", getCliente().getNome()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Chat c = chat.getDriver().getFunctions().getChatByNumber("554491050665");
            if (c != null) {
                c.sendMessage("*" + estabelecimento.getNomeEstabelecimento() + ":* Novo Pedido de Ajuda de " + this.getNome());
            }
            c = chat.getDriver().getFunctions().getChatByNumber("55" + Utilitarios.replaceAllNoDigit(estabelecimento.getNumeroAviso()));
            if (c != null) {
                c.sendMessage("*" + estabelecimento.getNomeEstabelecimento() + ":* Novo Pedido de Ajuda de " + this.getNome());
            }
        } catch (Exception ex) {

        }
        return true;
    }

    @Override
    public void onResume() {
        if (getChat().getDriver().getFunctions().isBusiness()) {
            getChat().removeLabel("Precisa de Ajuda");
        }
    }

    @Override
    public void processNewMsg(Message m) {
        if (m.getChat().getContact().getId().equals("554491050665@c.us")) {
        }
        if (m.getContent().toLowerCase().equals("/testar")) {
            m.getChat().sendMessage("Online");
            return;
        }
        if (!estabelecimento.isOpenChatBot()) {
            return;
        }
        if (m.getContent().trim().toLowerCase().equals("cancelar")) {
            setHandler(new HandlerAdeus(this), true);
            return;
        }
        getHandler().handle(m);
    }

    @Override
    public void processNewStatusV3Msg(Message message) {

    }

    public void finalizar() {
        if (this.checkMsgs != null && !this.checkMsgs.isCancelled() && !this.checkMsgs.isDone()) {
            this.checkMsgs.cancel(true);
        }
        if (this.checkMsgsStatusV3 != null && !this.checkMsgsStatusV3.isCancelled() && !this.checkMsgsStatusV3.isDone()) {
            this.checkMsgsStatusV3.cancel(true);
        }
        if (((HandlerBotDelivery) handler).notificaPedidosFechados()) {
            this.sendEncerramos();
        }
        this.executor.shutdown();
    }

    @Override
    public void atualizarChat(Chat chat) {
        this.chat = chat;
    }
}
