package ru.itis.mafia.impl;

import ru.itis.mafia.Room;
import ru.itis.mafia.inter.Handler;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

/** Класс для реагирования на действия пользователей */
public class HandlerImpl implements Handler {
    private static Room room;
    private static ArrayList<HandlerImpl> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    public HandlerImpl(Socket socket) {
        try {
            this.socket = socket;
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = bufferedReader.readLine();
            clientHandlers.add(this);
            broadcastMessage("SERVER: " + username + " подключился");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // хочу убрать
    @Override
    public void kill(String clientId) {

    }

    // хочу убрать
    @Override
    public void vote(String senderId, String receivedId) {

    }

    // тут будет обработка сообщений через switch case и вызов изменений в GIU (В классе Room)
    @Override
    public void run() {
        String messageFromClients;

        while (socket.isConnected()) {
            try {
                messageFromClients = bufferedReader.readLine();
                broadcastMessage(messageFromClients);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void broadcastMessage(String messageToSend) {
        for (HandlerImpl clientHandler : clientHandlers) {
            try {
                if (!clientHandler.username.equals(username)) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
