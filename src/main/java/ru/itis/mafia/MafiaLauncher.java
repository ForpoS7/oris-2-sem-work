package ru.itis.mafia;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Random;

public class MafiaLauncher extends Application {
    public static final String SERVER_IP = "127.0.0.1";
    public static final int SERVER_PORT = 1234;
    public static boolean gameIsStarted = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Создаем основной контейнер VBox
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);

        // Добавляем заголовок
        Button playButton = new Button("Готов");

        // Обработка нажатия на кнопку "Готов"
        playButton.setOnAction(event -> {
            try {
                // Создаем нового клиента и передаем ему Stage для отображения игры
                Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                listenForStartCommand(socket);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Добавляем элементы в контейнер
        layout.getChildren().addAll(new Label("Добро пожаловать в игру Мафия!"), playButton);

        // Создаем сцену и показываем окно
        Scene scene = new Scene(layout, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Мафия");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void listenForStartCommand(Socket socket) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (!gameIsStarted) {
                MafiaClient mafiaClient = new MafiaClient(socket);
                String message = bufferedReader.readLine();
                if (message != null) {
                    if (message.equals("/startGame")) {
                        Platform.runLater(() -> {
                            Stage gameStage = new Stage();
                            try {
                                mafiaClient.start(gameStage);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                        gameIsStarted = true;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}