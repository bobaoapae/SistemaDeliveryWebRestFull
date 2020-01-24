/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.handlersBot;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import modelo.ChatBot;
import modelo.GeoMessage;
import modelo.Message;
import sistemaDelivery.modelo.Endereco;
import utils.Utilitarios;

import java.net.URL;

/**
 * @author jvbor
 */
public class HandlerSolicitarEndereco extends HandlerBotDelivery {

    public HandlerSolicitarEndereco(ChatBot chat) {
        super(chat);
    }

    @Override
    protected boolean runFirstTime(Message m) {
        chat.getChat().sendMessage("Por favor, me envie o endereço para que eu possa entregar seu pedido").join();
        chat.getChat().markComposing(3000).join();
        chat.getChat().sendMessage(gerarObs("Envie tudo em uma unica mensagem. Ex: Av Tupassi, numero 10, casa verde de esquina...(O PONTO DE REFERÊNCIA É MUITO IMPORTANTE)")).join();
        return true;
    }

    @Override
    protected boolean runSecondTime(Message msg) {
        if (!(msg instanceof GeoMessage)) {
            Endereco endereco = new Endereco();
            endereco.setLogradouro(msg.getContent().trim());
            getChatBotDelivery().getPedidoAtual().setEndereco(endereco);
        } else if (msg instanceof GeoMessage) {
            try {
                String jsonAddres = Utilitarios.getText(new URL("https://maps.googleapis.com/maps/api/geocode/json?key=AIzaSyAKljZZxcZTXCkL4iwJtHHEWI42Fv1AZnI&latlng=" + ((GeoMessage) msg).getLatitude() + "," + ((GeoMessage) msg).getLongitude()));
                JsonParser jsonParser = new JsonParser();
                JsonElement element = jsonParser.parse(jsonAddres);
                String ende = element.getAsJsonObject().get("results").getAsJsonArray().get(0).getAsJsonObject().get("formatted_address").getAsString();
                Endereco endereco = new Endereco();
                endereco.setLogradouro(ende);
                getChatBotDelivery().getPedidoAtual().setEndereco(endereco);
            } catch (Exception ex) {
                chat.getChat().sendMessage("Desculpe, não consegui achar seu endereço com base na sua localização.").join();
                getChatBotDelivery().getChat().getDriver().onError(ex);
                return false;
            }
        } else {
            return false;
        }
        chat.setHandler(new HandlerConfirmarEndereco(chat), true);
        return true;
    }

    @Override
    public boolean notificaPedidosFechados() {
        return true;
    }

}
