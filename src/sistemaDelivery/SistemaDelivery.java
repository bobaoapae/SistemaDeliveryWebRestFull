package sistemaDelivery;

import driver.WebWhatsDriver;
import modelo.*;
import sistemaDelivery.controle.ControleChatsAsync;
import sistemaDelivery.controle.ControleEstabelecimentos;
import sistemaDelivery.controle.ControlePedidos;
import sistemaDelivery.handlersBot.HandlerBoasVindas;
import sistemaDelivery.handlersBot.HandlerBotDelivery;
import sistemaDelivery.modelo.ChatBotDelivery;
import sistemaDelivery.modelo.Estabelecimento;
import sistemaDelivery.modelo.EstadoPedido;
import sistemaDelivery.modelo.Pedido;
import utils.Utilitarios;

import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SistemaDelivery {
    private WebWhatsDriver driver;
    private ActionOnNeedQrCode onNeedQrCode;
    private ActionOnLowBaterry onLowBaterry;
    private ActionOnErrorInDriver onErrorInDriver;
    private Runnable onConnect, onDisconnect;
    private Estabelecimento estabelecimento;
    private SseBroadcaster broadcaster;
    private Sse sse;
    private ScheduledExecutorService executores = Executors.newSingleThreadScheduledExecutor();

    public SistemaDelivery(Estabelecimento estabelecimento) throws IOException {
        this.estabelecimento = estabelecimento;
        onConnect = () -> {
            for (Chat chat : driver.getFunctions().getAllNewChats()) {
                ControleChatsAsync.getInstance(estabelecimento).addChat(chat);
            }
            driver.getFunctions().setListennerToNewChat(chat -> ControleChatsAsync.getInstance(estabelecimento).addChat(chat));
        };
        onLowBaterry = (e) -> {
            if (broadcaster != null) {
                broadcaster.broadcast(sse.newEvent("low-battery", e + ""));
            }
        };
        this.driver = new WebWhatsDriver(estabelecimento.getUuid().toString(), false, onConnect, onNeedQrCode, onErrorInDriver, onLowBaterry, onDisconnect);
        executores.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (estabelecimento.isAbrirFecharPedidosAutomaticamente()) {
                    LocalTime horaAtual = LocalTime.now();
                    if (!estabelecimento.isOpenPedidos()) {
                        if (estabelecimento.getHoraAutomaticaAbrirPedidos().toLocalTime().isAfter(estabelecimento.getHoraAutomaticaFecharPedidos().toLocalTime())) {
                            if (!(horaAtual.isBefore(estabelecimento.getHoraAutomaticaAbrirPedidos().toLocalTime()) && horaAtual.isAfter(estabelecimento.getHoraAutomaticaFecharPedidos().toLocalTime()))) {
                                new Thread() {
                                    public void run() {
                                        abrirPedidos();
                                    }
                                }.start();
                            }
                        } else {
                            if (horaAtual.isAfter(estabelecimento.getHoraAutomaticaAbrirPedidos().toLocalTime()) && horaAtual.isBefore(estabelecimento.getHoraAutomaticaFecharPedidos().toLocalTime())) {
                                new Thread() {
                                    public void run() {
                                        abrirPedidos();
                                    }
                                }.start();
                            }
                        }
                    } else {
                        if (estabelecimento.getHoraAutomaticaFecharPedidos().toLocalTime().isBefore(estabelecimento.getHoraAutomaticaAbrirPedidos().toLocalTime())) {
                            if ((horaAtual.isBefore(estabelecimento.getHoraAutomaticaAbrirPedidos().toLocalTime()) && horaAtual.isAfter(estabelecimento.getHoraAutomaticaFecharPedidos().toLocalTime()))) {
                                new Thread() {
                                    public void run() {
                                        fecharPedidos();
                                    }
                                }.start();
                            }
                        } else {
                            if (horaAtual.isAfter(estabelecimento.getHoraAutomaticaFecharPedidos().toLocalTime()) || horaAtual.isBefore(estabelecimento.getHoraAutomaticaAbrirPedidos().toLocalTime())) {
                                new Thread() {
                                    public void run() {
                                        fecharPedidos();
                                    }
                                }.start();
                            }
                        }
                    }
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public boolean abrirPedidos() {
        try {
            estabelecimento.setOpenPedidos(true);
            if (!ControleEstabelecimentos.getInstance().salvarEstabelecimento(estabelecimento)) {
                return false;
            }
            Chat c = driver.getFunctions().getChatByNumber("554491050665");
            if (c != null) {
                c.sendMessage("*" + estabelecimento.getNomeEstabelecimento() + ":* Pedidos Aberto");
            }
            c = driver.getFunctions().getChatByNumber("55" + Utilitarios.plainText(estabelecimento.getNumeroAviso()));
            if (c != null) {
                c.sendMessage("*" + estabelecimento.getNomeEstabelecimento() + ":* Pedidos Aberto");
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean fecharPedidos() {
        try {
            estabelecimento.setOpenPedidos(false);
            if (!ControleEstabelecimentos.getInstance().salvarEstabelecimento(estabelecimento)) {
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        new Thread() {
            public void run() {
                for (ChatBotDelivery chat : ControleChatsAsync.getInstance(estabelecimento).getChats()) {
                    if (!((HandlerBotDelivery) chat.getHandler()).notificaPedidosFechados()) {
                        continue;
                    }
                    chat.sendEncerramos();
                    chat.setHandler(new HandlerBoasVindas(chat), false);
                }
            }
        }.start();
        List<Pedido> pedidosDoDia = ControlePedidos.getInstance().getPedidosDoDia(estabelecimento);
        long totalPedidoCancelados = pedidosDoDia.stream().filter(o -> ((Pedido) o).getEstadoPedido() == EstadoPedido.Cancelado).count();
        long totalPedidos = pedidosDoDia.size();
        long totalPedidosDelivery = pedidosDoDia.stream().filter(o -> o.isEntrega()).count();
        long totalPedidosDeliveryEntregues = pedidosDoDia.stream().filter(o -> ((Pedido) o).getEstadoPedido() == EstadoPedido.Concluido && o.isEntrega()).count();
        long totalPedidosDeliveryEmAberto = pedidosDoDia.stream().filter(o -> o.getEstadoPedido() != EstadoPedido.Cancelado && ((Pedido) o).getEstadoPedido() != EstadoPedido.Concluido && o.isEntrega()).count();
        long totalPedidosDeliveryCancelados = pedidosDoDia.stream().filter(o -> o.getEstadoPedido() == EstadoPedido.Cancelado && o.isEntrega()).count();
        long totalPedidosRetirada = pedidosDoDia.stream().filter(o -> !o.isEntrega()).count();
        long totalPedidosRetiradaEntregues = pedidosDoDia.stream().filter(o -> ((Pedido) o).getEstadoPedido() == EstadoPedido.Concluido && !o.isEntrega()).count();
        long totalPedidosRetiradaEmAberto = pedidosDoDia.stream().filter(o -> o.getEstadoPedido() != EstadoPedido.Cancelado && ((Pedido) o).getEstadoPedido() != EstadoPedido.Concluido && !o.isEntrega()).count();
        long totalPedidosRetiradaCancelados = pedidosDoDia.stream().filter(o -> o.getEstadoPedido() == EstadoPedido.Cancelado && !o.isEntrega()).count();

        long valorPedidos = 0;
        for (Pedido p : pedidosDoDia) {
            if (p.getEstadoPedido() == EstadoPedido.Cancelado) {
                continue;
            }
            valorPedidos += p.getTotal();
        }
        MessageBuilder builder = new MessageBuilder();
        builder.textBold(estabelecimento.getNomeEstabelecimento()).text(" - Resumo do Dia").newLine().newLine();
        builder.textBold("Total de Pedidos").text(": ").text(totalPedidos + "").newLine();
        builder.textBold("Total de Pedidos Cancelados").text(": ").text(totalPedidoCancelados + "").newLine().newLine();
        builder.textBold("Total de Pedidos Delivery").text(": ").text(totalPedidosDelivery + "").newLine();
        builder.textBold("Total de Pedidos Delivery Em Aberto").text(": ").text(totalPedidosDeliveryEmAberto + "").newLine();
        builder.textBold("Total de Pedidos Delivery Entregues").text(": ").text(totalPedidosDeliveryEntregues + "").newLine();
        builder.textBold("Total de Pedidos Delivery Cancelados").text(": ").text(totalPedidosDeliveryCancelados + "").newLine().newLine();
        builder.textBold("Total de Pedidos Retirada").text(": ").text(totalPedidosRetirada + "").newLine();
        builder.textBold("Total de Pedidos Retirada Em Aberto").text(": ").text(totalPedidosRetiradaEmAberto + "").newLine();
        builder.textBold("Total de Pedidos Retirada Concluidos").text(": ").text(totalPedidosRetiradaEntregues + "").newLine();
        builder.textBold("Total de Pedidos Retirada Cancelados").text(": ").text(totalPedidosRetiradaCancelados + "").newLine().newLine();
        if (valorPedidos > 0) {
            builder.textBold("Valor Total").text(": ").text(new DecimalFormat("###,###,###.00").format(valorPedidos) + "").newLine().newLine();
        }
        try {
            Chat c = driver.getFunctions().getChatByNumber("554491050665");
            if (c != null) {
                c.sendMessage("*" + estabelecimento.getNomeEstabelecimento() + ":* Pedidos Fechado");
                c.sendMessage(builder.build());
            }
            c = driver.getFunctions().getChatByNumber("55" + Utilitarios.plainText(estabelecimento.getNumeroAviso()));
            if (c != null) {
                c.sendMessage("*" + estabelecimento.getNomeEstabelecimento() + ":* Pedidos Fechado");
                c.sendMessage(builder.build());
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public Sse getSse() {
        return sse;
    }

    public void setSse(Sse sse) {
        this.sse = sse;
    }

    public SseBroadcaster getBroadcaster() {
        return broadcaster;
    }

    public void setBroadcaster(SseBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public WebWhatsDriver getDriver() {
        return driver;
    }

    public void finalizar() {
        if (driver.getBrowser().isDisposed()) {
            return;
        }
        if (broadcaster != null) {
            broadcaster.broadcast(sse.newEvent("logout", "ok"));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ControleChatsAsync.getInstance(estabelecimento).finalizar();
        driver.finalizar();
        driver.getBrowser().dispose();
    }

    public void logout() {
        driver.getFunctions().logout();
    }
}
