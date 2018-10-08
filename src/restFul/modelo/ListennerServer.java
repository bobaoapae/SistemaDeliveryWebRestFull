package restFul.modelo;

import restFul.controle.ControleSessions;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class ListennerServer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Sistema Delivery WhatsApp Web Iniciado");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Finalizando Sistema Delivery WhatsApp Web");
        ControleSessions.getInstance().finalizar();
    }
}
