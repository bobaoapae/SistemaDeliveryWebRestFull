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
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class ListennerServer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            Propriedades.inicializar(sce.getServletContext());
            File file = new File(Propriedades.pathLogs());
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
            logger.addHandler(fh);
            ControleSistema.getInstance().setLogger(logger);
            for (Estabelecimento estabelecimento : ControleEstabelecimentos.getInstance().getEstabelecimentosIniciarAutomaticamente()) {
                new Thread() {
                    public void run() {
                        try {
                            System.out.println("Iniciando para - " + estabelecimento.getNomeEstabelecimento());
                            ControleSessions.getInstance().getSessionForEstabelecimento(estabelecimento);
                            System.out.println("Iniciado para - " + estabelecimento.getNomeEstabelecimento());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        } catch (SQLException e) {
            ControleSistema.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Sistema Delivery WhatsApp Web Iniciado");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Finalizando Sistema Delivery WhatsApp Web");
        ControleSessions.getInstance().finalizar();
    }
}
