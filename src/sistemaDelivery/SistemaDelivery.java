package sistemaDelivery;

import driver.WebWhatsDriver;
import modelo.ActionOnErrorInDriver;
import modelo.ActionOnLowBaterry;
import modelo.ActionOnNeedQrCode;
import modelo.Chat;
import sistemaDelivery.controle.ControleChatsAsync;
import sistemaDelivery.modelo.Estabelecimento;

import java.io.IOException;

public class SistemaDelivery {
    private WebWhatsDriver driver;
    private ActionOnNeedQrCode onNeedQrCode;
    private ActionOnLowBaterry onLowBaterry;
    private ActionOnErrorInDriver onErrorInDriver;
    private Runnable onConnect, onDisconnect;
    private Estabelecimento estabelecimento;

    public SistemaDelivery(Estabelecimento estabelecimento) throws IOException {
        this.estabelecimento = estabelecimento;
        onConnect = () -> {
            for (Chat chat : driver.getFunctions().getAllNewChats()) {
                ControleChatsAsync.getInstance(estabelecimento).addChat(chat);
            }
            driver.getFunctions().setListennerToNewChat(chat -> ControleChatsAsync.getInstance(estabelecimento).addChat(chat));
        };
        this.driver = new WebWhatsDriver(estabelecimento.getUuid().toString(), false, onConnect, onNeedQrCode, onErrorInDriver, onLowBaterry, onDisconnect);
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
