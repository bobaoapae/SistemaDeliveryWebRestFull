package sistemaDelivery.handlersBot;

import modelo.ChatBot;
import modelo.Message;

public class HandlerPerguntarDesejaSerAvisadoQuandoAbrirOsPedidos extends HandlerBotDelivery {

    public HandlerPerguntarDesejaSerAvisadoQuandoAbrirOsPedidos(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message message) {
        chat.getChat().markComposing(2500);
        chat.getChat().sendMessage("Você gostaría de ser avisado quando iniciarmos o atendimento?");
        addOpcaoSim(null, s -> {
            chat.getChat().markComposing(2000);
            chat.getChat().sendMessage("Blz, assim que iniciarmos os atendimentos eu te aviso para você poder fazer o seu pedido.");
            getChatBotDelivery().setAvisarPedidoAbriu(true);
            chat.setHandler(new HandlerAdeus(chat), true);
        });
        addOpcaoNao(new HandlerAdeus(chat), null);
        return true;
    }

    @Override
    protected boolean runSecondTime(Message message) {
        return processarOpcoesMenu(message);
    }

    @Override
    public boolean notificaPedidosFechados() {
        return false;
    }

}
