package sistemaDelivery;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import driver.WebWhatsDriver;
import modelo.*;
import restFul.controle.ControleSessions;
import sistemaDelivery.controle.ControleChatsAsync;
import sistemaDelivery.controle.ControleClientes;
import sistemaDelivery.controle.ControleEstabelecimentos;
import sistemaDelivery.controle.ControlePedidos;
import sistemaDelivery.handlersBot.HandlerBoasVindas;
import sistemaDelivery.handlersBot.HandlerBotDelivery;
import sistemaDelivery.modelo.*;
import utils.Utilitarios;

import javax.swing.*;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import java.awt.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SistemaDelivery {
    private WebWhatsDriver driver;
    private ActionOnNeedQrCode onNeedQrCode;
    private ActionOnLowBattery onLowBaterry;
    private ActionOnErrorInDriver onErrorInDriver;
    private Runnable onConnect, onDisconnect;
    private Estabelecimento estabelecimento;
    private SseBroadcaster broadcaster, broadcasterWhats;
    private Sse sse, sseWhats;
    private ScheduledExecutorService executores = Executors.newScheduledThreadPool(5);
    private JsonParser parser;
    private Gson builder;
    private TelaWhatsApp telaWhatsApp;
    private LocalDateTime timeStart;

    public SistemaDelivery(Estabelecimento estabelecimento) throws IOException {
        timeStart = estabelecimento.getDataComHoraAtual();
        this.estabelecimento = estabelecimento;
        parser = new JsonParser();
        builder = Utilitarios.getDefaultGsonBuilder(null).create();
        onConnect = () -> {
            for (Chat chat : driver.getFunctions().getAllNewChats()) {
                ControleChatsAsync.getInstance(estabelecimento).addChat(chat);
            }
            driver.getFunctions().addListennerToNewChat(chat -> ControleChatsAsync.getInstance(estabelecimento).addChat(chat));
            driver.getFunctions().addListennerToNewChat(c -> {
                if (broadcasterWhats != null) {
                    JsonObject object = (JsonObject) builder.toJsonTree(parser.parse(c.toJson()));
                    object.add("contact", builder.toJsonTree(parser.parse(c.getContact().toJson())));
                    Cliente cliente = ControleClientes.getInstance().getClienteChatId(c.getId(), this.estabelecimento);
                    if (cliente != null) {
                        object.add("cliente", builder.toJsonTree(cliente));
                    }
                    broadcasterWhats.broadcast(sseWhats.newEvent("new-chat", builder.toJson(object)));
                }
            });
            driver.getFunctions().addListennerToNewMsg(new MessageObserverIncludeMe() {
                @Override
                public void onNewMsg(Message msg) {
                    if (broadcasterWhats != null) {
                        broadcasterWhats.broadcast(sseWhats.newEvent("new-msg", builder.toJson(parser.parse(msg.getJsObject().toJSONString()))));
                    }
                }
            });
        };
        onLowBaterry = (e) -> {
            if (broadcaster != null) {
                broadcaster.broadcast(sse.newEvent("low-battery", e + ""));
            }
        };
        telaWhatsApp = new TelaWhatsApp(estabelecimento);
        telaWhatsApp.setVisible(true);
        this.driver = new WebWhatsDriver(telaWhatsApp.getPanel(), "C:\\cache-web-whats\\" + estabelecimento.getUuid().toString(), false, onConnect, onNeedQrCode, onErrorInDriver, onLowBaterry, onDisconnect);
        executores.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if ((!estabelecimento.isOpenChatBot() || driver.getEstadoDriver() != EstadoDriver.LOGGED) && timeStart.plusMinutes(5).isBefore(estabelecimento.getDataComHoraAtual())) {
                    if (estabelecimento.isOpenChatBot()) {
                        estabelecimento.setOpenChatBot(false);
                        ControleEstabelecimentos.getInstance().salvarEstabelecimento(estabelecimento);
                    }
                    new Thread() {
                        public void run() {
                            ControleSessions.getInstance().finalizarSessionForEstabelecimento(estabelecimento);
                        }
                    }.start();
                }
                if (estabelecimento.isAbrirFecharPedidosAutomatico()) {
                    LocalTime horaAtual = estabelecimento.getHoraAtual();
                    if (!estabelecimento.isOpenPedidos()) {
                        if (estabelecimento.getHoraAutomaticaAbrirPedidos().toLocalTime().isAfter(estabelecimento.getHoraAutomaticaFecharPedidos().toLocalTime())) {
                            if (!(horaAtual.isBefore(estabelecimento.getHoraAutomaticaAbrirPedidos().toLocalTime()) && horaAtual.isAfter(estabelecimento.getHoraAutomaticaFecharPedidos().toLocalTime()))) {
                                abrirPedidos();
                            }
                        } else {
                            if (horaAtual.isAfter(estabelecimento.getHoraAutomaticaAbrirPedidos().toLocalTime()) && horaAtual.isBefore(estabelecimento.getHoraAutomaticaFecharPedidos().toLocalTime())) {
                                abrirPedidos();
                            }
                        }
                    } else {
                        if (estabelecimento.getHoraAutomaticaFecharPedidos().toLocalTime().isBefore(estabelecimento.getHoraAutomaticaAbrirPedidos().toLocalTime())) {
                            if ((horaAtual.isBefore(estabelecimento.getHoraAutomaticaAbrirPedidos().toLocalTime()) && horaAtual.isAfter(estabelecimento.getHoraAutomaticaFecharPedidos().toLocalTime()))) {
                                fecharPedidos();
                            }
                        } else {
                            if (horaAtual.isAfter(estabelecimento.getHoraAutomaticaFecharPedidos().toLocalTime()) || horaAtual.isBefore(estabelecimento.getHoraAutomaticaAbrirPedidos().toLocalTime())) {
                                fecharPedidos();
                            }
                        }
                    }
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
        executores.scheduleWithFixedDelay(() -> {
            if (broadcasterWhats != null) {
                broadcasterWhats.broadcast(sseWhats.newEvent("none"));
            }
            if (broadcaster != null) {
                broadcaster.broadcast(sse.newEvent("none"));
            }
        }, 0, 20, TimeUnit.SECONDS);
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

    public SseBroadcaster getBroadcasterWhats() {
        return broadcasterWhats;
    }

    public void setBroadcasterWhats(SseBroadcaster broadcasterWhats) {
        this.broadcasterWhats = broadcasterWhats;
    }

    public Sse getSseWhats() {
        return sseWhats;
    }

    public void setSseWhats(Sse sseWhats) {
        this.sseWhats = sseWhats;
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
        if (broadcaster != null) {
            broadcaster.broadcast(sse.newEvent("logout", "ok"));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            broadcaster.close();
            broadcaster = null;
        }
        if (telaWhatsApp != null) {
            telaWhatsApp.dispose();
        }
        if (executores != null && !executores.isShutdown()) {
            executores.shutdown();
        }
        ControleChatsAsync.getInstance(estabelecimento).finalizar();
        driver.finalizar();
    }

    public void logout() {
        driver.getFunctions().logout();
    }

    private class TelaWhatsApp extends JFrame {

        private JPanel panel;

        public TelaWhatsApp(Estabelecimento estabelecimento) {
            this.setTitle(estabelecimento.getNomeEstabelecimento() + " - " + estabelecimento.getUuid().toString().replaceAll("-", ""));
            this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            this.setExtendedState(JFrame.MAXIMIZED_BOTH);
            this.getContentPane().setLayout(new BorderLayout());
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            this.setMinimumSize(new Dimension(800, 600));
            this.setPreferredSize(new Dimension(800, 600));
            panel = new JPanel(new BorderLayout());
            this.add(panel);
            pack();
            this.setExtendedState(JFrame.ICONIFIED);
            this.setLocationRelativeTo(null);
        }

        public JPanel getPanel() {
            return panel;
        }
    }
}
