/*
 To change this license header, choose License Headers in Project Properties.
 To change this template file, choose Tools | Templates
 and open the template in the editor.
 */
package sistemaDelivery.controle;

import modelo.Chat;
import modelo.UserChat;
import sistemaDelivery.modelo.ChatBotDelivery;
import sistemaDelivery.modelo.Estabelecimento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author jvbor
 */
public class ControleChatsAsync {

    private static HashMap<Estabelecimento, ControleChatsAsync> instaces;
    private final List<ChatBotDelivery> chats;
    private Estabelecimento estabelecimento;

    public ControleChatsAsync(Estabelecimento estabelecimento) {
        this.estabelecimento = estabelecimento;
        this.chats = Collections.synchronizedList(new ArrayList<>());
    }


    public static ControleChatsAsync getInstance(Estabelecimento e) {
        if (instaces == null) {
            instaces = new HashMap<>();
            instaces.put(e, new ControleChatsAsync(e));
            return instaces.get(e);
        } else if (instaces.containsKey(e)) {
            return instaces.get(e);
        } else {
            instaces.put(e, new ControleChatsAsync(e));
            return instaces.get(e);
        }
    }

    public void finalizar() {
        for (ChatBotDelivery chatt : chats) {
            chatt.finalizar();
        }
        ControleChatsAsync.instaces.remove(this.estabelecimento);
    }

    public void addChat(Chat chat) {
        synchronized (chats) {
            try {
                if (chat instanceof UserChat) {
                    ChatBotDelivery chatt = new ChatBotDelivery(chat, estabelecimento, true);
                    chats.add(chatt);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public ChatBotDelivery getChatAsyncByChat(Chat chat) {
        for (ChatBotDelivery chatt : chats) {
            if (chatt.getChat().equals(chat)) {
                return chatt;
            }
        }
        return null;
    }

    public ChatBotDelivery getChatAsyncByChat(String chatid) {
        synchronized (chats) {
            for (ChatBotDelivery chatt : chats) {
                if (chatt.getChat().getId().equals(chatid)) {
                    return chatt;
                }
            }
            return null;
        }
    }

    public List<ChatBotDelivery> getChats() {
        return chats;
    }

}
