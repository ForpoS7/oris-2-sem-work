package ru.itis.mafia;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;

public class MafiaClient {
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private Socket socket;
    private String id;
    private String username;
    private boolean isDead = false;
    private String role;

    public MafiaClient(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.username = "Player " + new Random().nextInt(1000);
            this.id = UUID.randomUUID().toString();

            // Отправляем серверу имя пользователя
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 1234);
        MafiaClient client = new MafiaClient(socket);
        client.listenForMessage();
        client.sendMessage();
    }

    public void sendMessage() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (socket.isConnected()) {
                if (!isDead) {
                    String messageToSend = scanner.nextLine();

                    if (!messageToSend.startsWith("/")) {
                        bufferedWriter.write(username + ": " + messageToSend);
                    } else {
                        bufferedWriter.write(messageToSend);
                    }

                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                } else {
                    System.out.println("Вы мертвы и не можете отправлять сообщения.");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void listenForMessage() {
        new Thread(() -> {
            while (socket.isConnected()) {
                try {
                    String msgFromServer = bufferedReader.readLine();
                    if (msgFromServer == null) continue;

                    if (msgFromServer.startsWith("/set role")) {
                        this.role = msgFromServer.substring(10);
                        System.out.println("SERVER: Ваша роль - " + this.role);
                    } else if (msgFromServer.startsWith("/kill")) {
                        String killedPlayer = msgFromServer.substring(6);
                        if (username.equals(killedPlayer)) {
                            isDead = true;
                            System.out.println("SERVER: Вы были убиты!");
                        }
                    } else {
                        System.out.println(msgFromServer);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}

