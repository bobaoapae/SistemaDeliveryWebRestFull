package sistemaDelivery.handlersBot;

import handlersBot.HandlerBot;
import modelo.ChatBot;
import modelo.Message;

public class HandlerVoltar {

    private HandlerBot handlerVoltar;
    private Runnable acaoAoVoltar;
    private boolean resetar;

    public HandlerVoltar(HandlerBot handlerVoltar, Runnable acaoAoVoltar, boolean resetar) {
        this.handlerVoltar = handlerVoltar;
        this.acaoAoVoltar = acaoAoVoltar;
        this.resetar = resetar;
    }

    public void execute(ChatBot chatBot, Message m) {
        if (acaoAoVoltar != null) {
            acaoAoVoltar.run();
        }
        if (resetar) {
            handlerVoltar.reset();
        }
        chatBot.setHandler(handlerVoltar, true, m);
    }

    public HandlerBot getHandlerVoltar() {
        return handlerVoltar;
    }

    public void setHandlerVoltar(HandlerBot handlerVoltar) {
        this.handlerVoltar = handlerVoltar;
    }

    public Runnable getAcaoAoVoltar() {
        return acaoAoVoltar;
    }

    public void setAcaoAoVoltar(Runnable acaoAoVoltar) {
        this.acaoAoVoltar = acaoAoVoltar;
    }
}
