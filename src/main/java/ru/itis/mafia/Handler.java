package ru.itis.mafia;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class Handler implements Runnable{
    private static ArrayList<Handler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private String role;
    private String votePlayer;
    private boolean isDay;

    public Handler(Socket socket) {
        try {
            this.socket = socket;
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = bufferedReader.readLine();
            clientHandlers.add(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        String messageFromClients;

        while (socket.isConnected()) {
            try {
                messageFromClients = bufferedReader.readLine();
                if (messageFromClients.startsWith("/vote")) {
                    this.votePlayer = messageFromClients.substring(6);
                } else {
                    broadcastMessage(messageFromClients);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void broadcastMessage(String messageToSend) {
        for (Handler clientHandler : clientHandlers) {
            if (!clientHandler.username.equals(username)) {
                clientHandler.sendMessage(messageToSend);
            }
        }
    }

    public static void serverMessage(String message) {
        for (Handler client : clientHandlers) {
            client.sendMessage(message);
        }
    }

    public void sendMessage(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendPlayerMap() {
        StringBuilder sb = new StringBuilder("/playerMap");
        for (Handler clientHandler : clientHandlers) {
            sb.append("-")
                    .append(clientHandler.getUsername())
                    .append(":")
                    .append(clientHandler.getRole());
        }
        serverMessage(sb.toString());
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public String getVotePlayer() {
        return votePlayer;
    }

    public void setVotePlayer(String votePlayer) {
        this.votePlayer = votePlayer;
    }

    public void setDay(boolean day) {
        isDay = day;
    }

    public boolean isDay() {
        return isDay;
    }
}
