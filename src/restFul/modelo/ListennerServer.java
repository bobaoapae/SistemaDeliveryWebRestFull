package restFul.modelo;

import restFul.controle.ControleSessions;
import sistemaDelivery.controle.ControleEstabelecimentos;
import sistemaDelivery.modelo.Estabelecimento;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;

@WebListener
public class ListennerServer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        for (Estabelecimento estabelecimento : ControleEstabelecimentos.getInstance().getEstabelecimentosChatBotAberto()) {
            try {
                ControleSessions.getInstance().getSessionForEstabelecimento(estabelecimento);
                System.out.println("Iniciado para - " + estabelecimento.getNomeEstabelecimento());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Sistema Delivery WhatsApp Web Iniciado");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Finalizando Sistema Delivery WhatsApp Web");
        ControleSessions.getInstance().finalizar();
    }
}
