package restFul.controle;

import sistemaDelivery.SistemaDelivery;
import sistemaDelivery.modelo.Estabelecimento;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ControleSessions {

    private static ControleSessions instance;
    private Map<Estabelecimento, SistemaDelivery> sessions;
    private static final Object syncroniseGetInstance = new Object();
    private static boolean finalizado = false;

    private ControleSessions() {
        this.sessions = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleSessions getInstance() {
        synchronized (syncroniseGetInstance) {
            if (instance == null) {
                instance = new ControleSessions();
            }
            return instance;
        }
    }

    public boolean checkSessionAtiva(Estabelecimento e) {
        synchronized (sessions) {
            return sessions.containsKey(e);
        }
    }

    public SistemaDelivery getSessionForEstabelecimento(Estabelecimento estabelecimento) throws IOException {
        if (finalizado) {
            return null;
        }
        synchronized (sessions) {
            if (!sessions.containsKey(estabelecimento)) {
                sessions.put(estabelecimento, new SistemaDelivery(estabelecimento));
            }
            return sessions.get(estabelecimento);
        }
    }

    public void finalizar() {
        if (finalizado) {
            return;
        }
        finalizado = true;
        synchronized (sessions) {
            for (Map.Entry<Estabelecimento, SistemaDelivery> entry : sessions.entrySet()) {
                entry.getValue().finalizar();
                System.out.println("Finalizado para - " + entry.getKey().getNomeEstabelecimento());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            sessions.clear();
        }
    }

    public void finalizarSessionForEstabelecimento(Estabelecimento estabelecimento) {
        if (finalizado) {
            return;
        }
        synchronized (sessions) {
            if (sessions.containsKey(estabelecimento)) {
                sessions.get(estabelecimento).finalizar();
                sessions.remove(estabelecimento);
            }
        }
    }
}
