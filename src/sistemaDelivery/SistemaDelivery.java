package sistemaDelivery;

import driver.WebWhatsDriver;
import modelo.ActionOnErrorInDriver;
import modelo.ActionOnLowBaterry;
import modelo.ActionOnNeedQrCode;
import modelo.Chat;
import sistemaDelivery.controle.ControleChatsAsync;
import sistemaDelivery.modelo.Estabelecimento;

import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import java.io.IOException;

public class SistemaDelivery {
    private WebWhatsDriver driver;
    private ActionOnNeedQrCode onNeedQrCode;
    private ActionOnLowBaterry onLowBaterry;
    private ActionOnErrorInDriver onErrorInDriver;
    private Runnable onConnect, onDisconnect;
    private Estabelecimento estabelecimento;
    private SseBroadcaster broadcaster;
    private Sse sse;

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
        ControleChatsAsync.getInstance(estabelecimento).finalizar();
        driver.finalizar();
        driver.getBrowser().dispose();
    }

    public void logout() {

    }
}
