package ru.itis.mafia;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Random;

public class MafiaClient extends Application {
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private Socket socket;
    private String username;
    private TextArea chatArea;

    public MafiaClient (Socket socket){
        try {
            this.socket = socket;
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.username = "Player " + new Random().nextInt(1000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Отправляем имя пользователя на сервер
        sendUsername();
        // Запускаем поток для приема сообщений
        listenForMessage();

        // Создаем основной контейнер VBox
        VBox root = new VBox(10); // Отступы между элементами 10 пикселей
        root.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0;");

        // Заголовок окна
        primaryStage.setTitle("Mafia Game - " + username);

        // Поле для чата
        chatArea = new TextArea();
        chatArea.setEditable(false); // Чат только для чтения
        chatArea.setPrefRowCount(10);

        // Поле ввода сообщения и кнопка отправки
        TextField messageField = new TextField();
        messageField.setOnAction(event -> {
            String message = messageField.getText();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageField.clear();
            }
        });

        // Группируем поле ввода и кнопку отправки
        HBox inputBox = new HBox(10);
        inputBox.getChildren().addAll(messageField);

        // Добавляем все элементы в корневой контейнер
        root.getChildren().addAll(
                new Label("Chat:"),
                chatArea,
                inputBox
        );

        // Создаем сцену и показываем окно
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void sendUsername() {
        try {
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(String message) {
        try {
            bufferedWriter.write(username + ": " + message);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            Platform.runLater(() -> chatArea.appendText("You" + ": " + message + "\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void listenForMessage() {
        new Thread(() -> {
            while (socket.isConnected()) {
                try {
                    String msgFromGroupChat = bufferedReader.readLine();
                    if (msgFromGroupChat != null) {
                        // Обновляем чат через Platform.runLater()
                        Platform.runLater(() -> chatArea.appendText(msgFromGroupChat + "\n"));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}
