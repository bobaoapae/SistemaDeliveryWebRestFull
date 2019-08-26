package restFul.modelo;

import restFul.controle.ControleSessions;
import restFul.controle.ControleSistema;
import sistemaDelivery.controle.ControleEstabelecimentos;
import sistemaDelivery.modelo.Estabelecimento;
import utils.Propriedades;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;
import java.util.logging.*;

@WebListener
public class ListennerServer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            Propriedades.inicializar(sce.getServletContext());
            File file = new File(Propriedades.pathCacheWebWhats());
            if (!file.exists()) {
                file.mkdir();
            }
            Logger logger = Logger.getLogger("LogGeral");
            file = new File(Propriedades.pathLogs());
            if (!file.exists()) {
                file.mkdir();
            }
            file = new File(Propriedades.pathBinarios());
            if (!file.exists()) {
                file.mkdir();
            }
            System.setProperty("jxbrowser.chromium.dir", Propriedades.pathBinarios());
            FileHandler fh = new FileHandler(Propriedades.pathLogs() + "LogGeral.txt", true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(new StreamHandler(System.out, new SimpleFormatter()));
            logger.addHandler(fh);
            ControleSistema.getInstance().setLogger(logger);
            if (Propriedades.getEstadoServidor() != Propriedades.EstadoServidor.TESTES) {
                for (Estabelecimento estabelecimento : ControleEstabelecimentos.getInstance().getEstabelecimentosIniciarAutomaticamente()) {
                    new Thread() {
                        public void run() {
                            try {
                                System.out.println("Iniciando para - " + estabelecimento.getNomeEstabelecimento());
                                ControleSessions.getInstance().getSessionForEstabelecimento(estabelecimento);
                                System.out.println("Iniciado para - " + estabelecimento.getNomeEstabelecimento());
                            } catch (Exception e) {
                                ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                    }.start();
                }
            }
        } catch (Exception e) {
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
        System.out.println("Sistema Delivery WhatsApp Web Iniciado");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Finalizando Sistema Delivery WhatsApp Web");
        new Thread(() -> ControleSessions.getInstance().finalizar()).start();
    }
}
