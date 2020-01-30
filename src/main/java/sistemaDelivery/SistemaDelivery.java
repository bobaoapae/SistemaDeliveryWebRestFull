package sistemaDelivery;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import driver.WebWhatsDriver;
import modelo.*;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import restFul.controle.ControleSessions;
import sistemaDelivery.controle.ControleChatsAsync;
import sistemaDelivery.controle.ControleClientes;
import sistemaDelivery.controle.ControleEstabelecimentos;
import sistemaDelivery.controle.ControlePedidos;
import sistemaDelivery.handlersBot.HandlerBoasVindas;
import sistemaDelivery.handlersBot.HandlerBotDelivery;
import sistemaDelivery.jobs.AbrirPedidoJob;
import sistemaDelivery.jobs.FecharPedidoJob;
import sistemaDelivery.modelo.*;
import utils.Propriedades;
import utils.Utilitarios;
import utils.Utils;

import javax.swing.*;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class SistemaDelivery {
    private static HashMap<Estabelecimento, Logger> loggers = new HashMap<>();
    private WebWhatsDriver driver;
    private ActionOnNeedQrCode onNeedQrCode;
    private ActionOnLowBattery onLowBaterry;
    private ActionOnErrorInDriver onErrorInDriver;
    private ActionOnWhatsAppVersionMismatch onWhatsAppVersionMismatch;
    private Runnable onConnect, onDisconnect;
    private Estabelecimento estabelecimento;
    private SseBroadcaster broadcaster, broadcasterWhats;
    private Sse sse, sseWhats;
    private ScheduledExecutorService executores = Executors.newScheduledThreadPool(5);
    private JsonParser parser;
    private Gson builder;
    private TelaWhatsApp telaWhatsApp;
    private Logger logger;
    private StdSchedulerFactory schedulerFactory;
    private Scheduler scheduler;
    private List<SseEventSink> listennersEventosDelivery;

    public SistemaDelivery(Estabelecimento estabelecimento, boolean headless) throws IOException {
        listennersEventosDelivery = Collections.synchronizedList(new ArrayList<>());
        logger = SistemaDelivery.createOrGetLogger(estabelecimento);
        this.estabelecimento = estabelecimento;
        parser = new JsonParser();
        builder = Utilitarios.getDefaultGsonBuilder(null).create();
        onConnect = () -> {
            ControleChatsAsync.getInstance(estabelecimento).finalizar();
            if (!estabelecimento.isIniciarAutomaticamente()) {
                estabelecimento.setIniciarAutomaticamente(true);
                try {
                    ControleEstabelecimentos.getInstance().salvarEstabelecimento(estabelecimento);
                } catch (SQLException e) {
                    Logger.getLogger(estabelecimento.getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
                }
            }
            driver.getFunctions().getAllChats(false).thenAccept(chats -> {
                for (Chat chat : chats) {
                    ControleChatsAsync.getInstance(estabelecimento).addChat(chat);
                }
            });
            driver.getFunctions().addChatListenner(chat -> ControleChatsAsync.getInstance(estabelecimento).addChat(chat), EventType.ADD, false);
            driver.getFunctions().addChatListenner(chat -> ControleChatsAsync.getInstance(estabelecimento).removeChat(chat), EventType.REMOVE, true);
            driver.getFunctions().addChatListenner(c -> {
                JsonObject object = (JsonObject) builder.toJsonTree(parser.parse(c.toJson()));
                object.add("contact", builder.toJsonTree(parser.parse(c.getContact().toJson())));
                Cliente cliente = null;
                try {
                    cliente = ControleClientes.getInstance().getClienteChatId(c.getId(), this.estabelecimento);
                } catch (SQLException e) {
                    Logger.getLogger(estabelecimento.getUuid().toString()).log(Level.SEVERE, e.getMessage(), e);
                }
                if (cliente != null) {
                    object.add("cliente", builder.toJsonTree(cliente));
                }
                enviarEventoWpp(TipoEventoWpp.NEW_CHAT, builder.toJson(object));
            }, EventType.ADD);
            driver.getFunctions().addMsgListenner(new MessageObserverIncludeMe(MessageObserver.MsgType.CHAT) {
                @Override
                public void run(Message m) {
                    enviarEventoWpp(TipoEventoWpp.NEW_MSG, builder.toJson(parser.parse(m.toJson())));
                }
            }, EventType.ADD);
        };
        onLowBaterry = (e) -> {
            enviarEventoWpp(TipoEventoWpp.LOW_BATTERY, e + "");
        };
        onNeedQrCode = (e) -> {
            enviarEventoWpp(TipoEventoWpp.NEED_QRCODE, e);
        };
        onErrorInDriver = (e) -> {
            logger.log(Level.SEVERE, e.getMessage(), e);
        };
        onWhatsAppVersionMismatch = (targetVersion, actualVersion) -> {
            logger.log(Level.SEVERE, "Mudança na versão do WhatsApp - Versão do WhatsApp: " + targetVersion.toString() + " - Versão da Lib:" + actualVersion.toString());
        };
        if (!headless) {
            telaWhatsApp = new TelaWhatsApp(estabelecimento);
            telaWhatsApp.setVisible(true);
            this.driver = new WebWhatsDriver(telaWhatsApp.getPanel(), Propriedades.pathCacheWebWhats() + estabelecimento.getUuid().toString(), onConnect, onNeedQrCode, onErrorInDriver, onLowBaterry, onDisconnect, null, onWhatsAppVersionMismatch);
        } else {
            this.driver = new WebWhatsDriver(Propriedades.pathCacheWebWhats() + estabelecimento.getUuid().toString(), onConnect, onNeedQrCode, onErrorInDriver, onLowBaterry, onDisconnect, null, onWhatsAppVersionMismatch);
        }
        executores.scheduleWithFixedDelay(() -> {
            if (broadcasterWhats != null) {
                broadcasterWhats.broadcast(sseWhats.newEvent("none"));
            }
            if (broadcaster != null) {
                broadcaster.broadcast(sse.newEvent("none"));
            }
        }, 0, 20, TimeUnit.SECONDS);
        executores.scheduleWithFixedDelay(() -> {
            if ((!estabelecimento.isOpenChatBot() || driver.getEstadoDriver() != EstadoDriver.LOGGED)) {
                if (estabelecimento.isIniciarAutomaticamente()) {
                    estabelecimento.setIniciarAutomaticamente(false);
                    try {
                        ControleEstabelecimentos.getInstance().salvarEstabelecimento(estabelecimento);
                    } catch (SQLException e) {
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
                new Thread() {
                    public void run() {
                        ControleSessions.getInstance().finalizarSessionForEstabelecimento(estabelecimento);
                    }
                }.start();
            }
        }, 5, 5, TimeUnit.MINUTES);
        schedulerFactory = new StdSchedulerFactory();
        Properties properties = new Properties();
        properties.put("org.quartz.scheduler.instanceName", estabelecimento.getNomeEstabelecimento() + estabelecimento.getUuid());
        properties.put("org.quartz.scheduler.instanceId", "AUTO");
        properties.put("org.quartz.threadPool.threadCount", "2");
        try {
            schedulerFactory.initialize(properties);
            scheduler = schedulerFactory.getScheduler();
            scheduler.start();
            atualizarJobsHorariosFuncionamento();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        if (estabelecimento.isAbrirFecharPedidosAutomatico()) {
            LocalDateTime localDateTime = estabelecimento.getDataComHoraAtual();
            try {
                if (estabelecimento.isTimeBeetwenHorarioFuncionamento(localDateTime.toLocalTime(), localDateTime.getDayOfWeek())) {
                    abrirPedidos();
                } else {
                    fecharPedidos();
                }
            } catch (SQLException s) {
                logger.log(Level.SEVERE, s.getMessage(), s);
            }
        }
    }

    private static Logger createOrGetLogger(Estabelecimento estabelecimento) throws IOException {
        if (!loggers.containsKey(estabelecimento)) {
            try {
                Logger logger = Logger.getLogger(estabelecimento.getUuid().toString());
                FileHandler fh = new FileHandler(Propriedades.pathLogs() + estabelecimento.getNomeEstabelecimento() + " - " + estabelecimento.getUuid().toString().replaceAll("-", "") + ".txt", true);
                logger.addHandler(fh);
            /*logger.addHandler(new Handler() {
                @Override
                public void publish(LogRecord lr) {
                    try {
                        if (driver != null && driver.getEstadoDriver() != null && driver.getEstadoDriver() == EstadoDriver.LOGGED) {
                            Chat c = driver.getFunctions().getChatByNumber("554491050665");
                            if (c != null) {
                                c.sendMessage("*" + estabelecimento.getNomeEstabelecimento() + ":* Erro-> " + ExceptionUtils.getStackTrace(lr.getThrown()));
                            }
                        }
                    } catch (Exception ex) {

                    }
                }

                @Override
                public void flush() {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public void close() throws SecurityException {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });*/
                logger.addHandler(new StreamHandler(System.out, new SimpleFormatter()));
                SimpleFormatter formatter = new SimpleFormatter();
                fh.setFormatter(formatter);
                loggers.put(estabelecimento, logger);
            } catch (SecurityException e) {
                throw e;
            } catch (IOException e) {
                throw e;
            }
        }
        return loggers.get(estabelecimento);
    }

    public boolean abrirPedidos() throws SQLException {
        if (estabelecimento.isOpenPedidos()) {
            return true;
        }
        try {
            estabelecimento.setOpenPedidos(true);
            if (!ControleEstabelecimentos.getInstance().salvarEstabelecimento(estabelecimento)) {
                return false;
            }
            ControleChatsAsync.getInstance(estabelecimento).getChats().stream().filter(ChatBotDelivery::isAvisarPedidoAbriu).forEach(chatBotDelivery -> {
                chatBotDelivery.setAvisarPedidoAbriu(false);
                chatBotDelivery.getChat().sendMessage("Olá, estou passando para avisar que já iniciamos nosso atendimento. Caso queira fazer um pedido basta me mandar uma mensagem blz!?");
            });
            driver.getFunctions().getChatByNumber("554491050665").thenCompose(chat -> {
                if (chat != null) {
                    return chat.sendMessage("*" + estabelecimento.getNomeEstabelecimento() + ":* Pedidos Aberto").thenApply(jsValue -> {
                        return chat;
                    });
                } else {
                    return CompletableFuture.failedFuture(new RuntimeException("Chat Não Encontrado"));
                }
            }).thenCompose(chat -> {
                return chat.setArchive(true);
            });
            driver.getFunctions().getChatByNumber("55" + Utils.retornarApenasNumeros(estabelecimento.getNumeroAviso())).thenCompose(chat -> {
                if (chat != null) {
                    return chat.sendMessage("*" + estabelecimento.getNomeEstabelecimento() + ":* Pedidos Aberto");
                } else {
                    return CompletableFuture.failedFuture(new RuntimeException("Chat Não Encontrado"));
                }
            });
            return true;
        } catch (Exception ex) {
            throw ex;
        }
    }

    public boolean fecharPedidos() throws SQLException {
        if (!estabelecimento.isOpenPedidos()) {
            return true;
        }
        try {
            estabelecimento.setOpenPedidos(false);
            if (!ControleEstabelecimentos.getInstance().salvarEstabelecimento(estabelecimento)) {
                return false;
            }
        } catch (Exception ex) {
            throw ex;
        }
        new Thread() {
            public void run() {
                List<ChatBotDelivery> chats = ControleChatsAsync.getInstance(estabelecimento).getChats();
                synchronized (chats) {
                    for (ChatBotDelivery chat : chats) {
                        if (!((HandlerBotDelivery) chat.getHandler()).notificaPedidosFechados()) {
                            continue;
                        }
                        chat.sendEncerramos();
                        chat.setHandler(new HandlerBoasVindas(chat), false);
                    }
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
        driver.getFunctions().getChatByNumber("554491050665").thenCompose(chat -> {
            if (chat != null) {
                return chat.sendMessage("*" + estabelecimento.getNomeEstabelecimento() + ":* Pedidos Fechado").thenCompose(jsValue -> {
                    return chat.sendMessage(builder.build());
                }).thenCompose(jsValue -> {
                    return chat.setArchive(true);
                });
            } else {
                return CompletableFuture.failedFuture(new RuntimeException("Chat Não Encontrado"));
            }
        });
        driver.getFunctions().getChatByNumber("55" + Utils.retornarApenasNumeros(estabelecimento.getNumeroAviso())).thenCompose(chat -> {
            if (chat != null) {
                return chat.sendMessage("*" + estabelecimento.getNomeEstabelecimento() + ":* Pedidos Fechado").thenCompose(jsValue -> {
                    return chat.sendMessage(builder.build());
                }).thenCompose(jsValue -> {
                    return chat.setArchive(true);
                });
            } else {
                return CompletableFuture.failedFuture(new RuntimeException("Chat Não Encontrado"));
            }
        });
        return true;
    }

    public Estabelecimento getEstabelecimento() {
        return estabelecimento;
    }

    public int getUsuariosAtivos() {
        int total = 0;
        List<ChatBotDelivery> chats = ControleChatsAsync.getInstance(estabelecimento).getChats();
        synchronized (chats) {
            for (ChatBotDelivery chat : chats) {
                if (!((HandlerBotDelivery) chat.getHandler()).notificaPedidosFechados()) {
                    continue;
                }
                total++;
            }
        }
        return total;
    }

    public void atualizarJobsHorariosFuncionamento() throws SchedulerException {
        Map<DayOfWeek, List<HorarioFuncionamento>> horarioFuncionamentos = estabelecimento.getHorariosFuncionamento();
        if (scheduler != null) {
            scheduler.clear();
        }
        if (estabelecimento.isAbrirFecharPedidosAutomatico()) {
            synchronized (horarioFuncionamentos) {
                for (Map.Entry<DayOfWeek, List<HorarioFuncionamento>> enty : horarioFuncionamentos.entrySet()) {
                    List<HorarioFuncionamento> horariosDoDia = enty.getValue();
                    synchronized (horariosDoDia) {
                        for (HorarioFuncionamento horario : horariosDoDia) {
                            if (!horario.isAtivo()) {
                                continue;
                            }
                            JobDetail jobAbrir = JobBuilder.newJob(AbrirPedidoJob.class)
                                    .withIdentity("abrirPedidoJob" + horario.getUuid(), "abrirFecharPedidos")
                                    .build();
                            jobAbrir.getJobDataMap().put("sistemaDelivery", this);
                            JobDetail jobFechar = JobBuilder.newJob(FecharPedidoJob.class)
                                    .withIdentity("fecharPedidoJob" + horario.getUuid(), "abrirFecharPedidos")
                                    .build();
                            jobFechar.getJobDataMap().put("sistemaDelivery", this);
                            Trigger triggerAbrir = TriggerBuilder.newTrigger()
                                    .withIdentity("abrirPedidoTrigger" + horario.getUuid(), "abrirFecharPedidos")
                                    .withSchedule(CronScheduleBuilder.cronSchedule("0 " + horario.getHoraAbrir().getMinute() + " " + horario.getHoraAbrir().getHour() + " ? * " + horario.getDiaDaSemana().getDisplayName(TextStyle.SHORT_STANDALONE, Locale.forLanguageTag("en-US")).toUpperCase() + " *").inTimeZone(estabelecimento.getTimeZoneObject()))
                                    .build();
                            Trigger triggerFechar = TriggerBuilder.newTrigger()
                                    .withIdentity("fecharPedidoTrigger" + horario.getUuid(), "abrirFecharPedidos")
                                    .withSchedule(CronScheduleBuilder.cronSchedule("0 " + horario.getHoraFechar().getMinute() + " " + horario.getHoraFechar().getHour() + " ? * " + horario.getDiaDaSemana().getDisplayName(TextStyle.SHORT_STANDALONE, Locale.forLanguageTag("en-US")).toUpperCase() + " *").inTimeZone(estabelecimento.getTimeZoneObject()))
                                    .build();
                            scheduler.scheduleJob(jobAbrir, triggerAbrir);
                            scheduler.scheduleJob(jobFechar, triggerFechar);
                        }
                    }
                }
            }
        }
    }

    public WebWhatsDriver getDriver() {
        return driver;
    }

    public Logger getLogger() {
        return logger;
    }

    public void finalizar() {
        if (broadcaster != null) {
            broadcaster.broadcast(sse.newEvent("logout", "ok"));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
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
        try {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
            }
        } catch (SchedulerException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        ControleChatsAsync.getInstance(estabelecimento).finalizar();
        driver.finalizar();
    }

    public void logout() {
        driver.getFunctions().logout();
    }

    public void enviarMesagemParaTecnico(String msg) {
        String mensagem = "*" + estabelecimento.getNomeEstabelecimento() + ":* " + msg;
        driver.getFunctions().getChatByNumber("554491050665").thenCompose(chat -> {
            if (chat != null) {
                return chat.sendMessage(mensagem).thenCompose(jsValue -> {
                    return chat.setArchive(true);
                });
            } else {
                return CompletableFuture.failedFuture(new RuntimeException("Chat Não Encontrado"));
            }
        });
    }

    public void enviarMensagemParaSuporte(String msg) {
        String mensagem = "*" + estabelecimento.getNomeEstabelecimento() + ":* " + msg;
        enviarMesagemParaTecnico(mensagem);
        driver.getFunctions().getChatByNumber("55" + Utils.retornarApenasNumeros(estabelecimento.getNumeroAviso())).thenCompose(chat -> {
            if (chat != null) {
                return chat.sendMessage(mensagem);
            } else {
                return CompletableFuture.failedFuture(new RuntimeException("Chat Não Econtrado"));
            }
        });
    }

    public CompletionStage<?> enviarEventoDelivery(TipoEventoDelivery tipoEventoDelivery, String dado) {
        if ((tipoEventoDelivery == TipoEventoDelivery.NOVO_PEDIDO || tipoEventoDelivery == TipoEventoDelivery.NOVA_RESERVA) && !possuiListennersDelivery()) {
            String msg = tipoEventoDelivery == TipoEventoDelivery.NOVO_PEDIDO ? "um novo pedido" : "uma nova reserva";
            String mensagem = "Recebeu " + msg + ", porém o sistema de impressão e o site estão desconectados.\n\n" +
                    "Por favor acesse o site ou ligue o sistema de impressão para não perder nenhum pedido.";
            enviarMensagemParaSuporte(mensagem);
        }
        if (broadcaster != null) {
            return broadcaster.broadcast(sse.newEvent(tipoEventoDelivery.toString().toLowerCase().replace('_', '-'), dado));
        }
        return null;
    }

    public CompletionStage<?> enviarEventoWpp(TipoEventoWpp tipoEventoWpp, String dado) {
        if (broadcasterWhats != null) {
            return broadcasterWhats.broadcast(sseWhats.newEvent(tipoEventoWpp.toString().toLowerCase().replace('_', '-'), dado));
        }
        return null;
    }

    public void registrarListennerEventoDelivery(SseEventSink sink) {
        broadcaster.register(sink);
        synchronized (listennersEventosDelivery) {
            listennersEventosDelivery.add(sink);
        }
    }

    public void registrarListennerEventoWpp(SseEventSink sink) {
        broadcasterWhats.register(sink);
    }

    public void inicializarEventosDelivery(Sse sse) {
        if (broadcaster == null) {
            broadcaster = sse.newBroadcaster();
            broadcaster.onClose(sseEventSink -> {
                synchronized (listennersEventosDelivery) {
                    listennersEventosDelivery.remove(sseEventSink);
                }
            });
            this.sse = sse;
        }
    }

    public void inicializarEventosWpp(Sse sse) {
        if (broadcasterWhats == null) {
            broadcasterWhats = sse.newBroadcaster();
            this.sseWhats = sse;
        }
    }

    public boolean possuiListennersDelivery() {
        synchronized (listennersEventosDelivery) {
            return !listennersEventosDelivery.isEmpty();
        }
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

    public enum TipoEventoDelivery {
        NOVO_PEDIDO,
        NOVA_RESERVA,
        ATUALIZAR_PEDIDO,
        PEDIDO_AJUDA
    }

    public enum TipoEventoWpp {
        CHAT_UPDATE,
        NEW_CHAT,
        NEW_MSG,
        NEW_MSG_V3,
        LOW_BATTERY,
        NEED_QRCODE
    }
}
