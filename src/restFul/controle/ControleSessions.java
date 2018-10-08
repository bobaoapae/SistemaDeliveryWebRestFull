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
    private static final Object syncronizeGetSession = new Object();
    private ControleSessions() {
        this.sessions = Collections.synchronizedMap(new HashMap<>());
    }

    public static ControleSessions getInstance() {
        synchronized (syncronizeGetSession) {
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
        synchronized (sessions) {
            if (!sessions.containsKey(estabelecimento)) {
                sessions.put(estabelecimento, new SistemaDelivery(estabelecimento));
            }
            return sessions.get(estabelecimento);
        }
    }

    public void finalizar() {
        synchronized (sessions) {
            for (Map.Entry<Estabelecimento, SistemaDelivery> entry : sessions.entrySet()) {
                entry.getValue().finalizar();
            }
            sessions.clear();
        }
    }

    public void finalizarSessionForEstabelecimento(Estabelecimento estabelecimento) {
        synchronized (sessions) {
            if (sessions.containsKey(estabelecimento)) {
                sessions.get(estabelecimento).finalizar();
                sessions.remove(estabelecimento);
            }
        }
    }
}
