package ru.itis.mafia;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class Handler implements Runnable {
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
            broadcastMessage("SERVER: " + username + " подключился");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRole(String role) {
        this.role = role;
        sendMessage("/set role " + role);
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

    @Override
    public void run() {
        while (socket.isConnected()) {
            try {
                String message = bufferedReader.readLine();
                if (message.startsWith("/vote")) {
                    this.votePlayer = message.substring(6);
                } else {
                    broadcastMessage(message);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void broadcastMessage(String message) {
        for (Handler client : clientHandlers) {
            if (!client.username.equals(username)) {
                client.sendMessage(message);
            }
        }
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

    public static void serverMessage(String message) {
        for (Handler client : clientHandlers) {
            client.sendMessage(message);
        }
    }

    public void setDay(boolean isDay) {
        this.isDay = isDay;
    }
}
