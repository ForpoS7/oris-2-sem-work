package ru.itis.mafia;

import ru.itis.mafia.impl.HandlerImpl;
import ru.itis.mafia.inter.Handler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MafiaServer {
    public static final int PORT = 1234;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Сервер стартанул");
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                System.out.println("Клиент подключился");
                Handler handler = new HandlerImpl(socket);

                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
