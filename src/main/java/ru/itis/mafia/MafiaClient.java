package ru.itis.mafia;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MafiaClient extends Application {
    private ComboBox<String> voteComboBox;
    private BorderPane root;
    private VBox votePanel;
    private Timeline timeline;
    private TextArea chatArea;
    private Map<String, String> playerMap = new HashMap<>();
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private Socket socket;

    private static final double AVATAR_SIZE = 100; // Размер аватарки
    private static final String DEFAULT_AVATAR = "/Игрок.png"; // Общая аватарка
    private static final String MAFIA_AVATAR = "/Мафия.png"; // Аватарка мафии
    private static final String PEACEFUL_AVATAR = "/Мирный.png"; // Аватарка мирного жителя
    private static final int MAX_PLAYERS = 4;
    private static final int GRID_COLUMNS = MAX_PLAYERS / 2; // Количество колонок в сетке

    private String username;
    private boolean isDead = false;
    // Флаг для управления состоянием таймера
    private boolean isGameRunning = true;

    public MafiaClient(Socket socket) {
        try {
            this.socket = socket;
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            username = "Игрок " + new Random().nextInt(1000);

            // Отправляем серверу имя пользователя
            sendUsername();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Заголовок окна
        primaryStage.setTitle("Игра Мафия - " + username);

        // Главный контейнер (BorderPane)
        root = new BorderPane();
        root.setPadding(new Insets(10));

        // Правая часть: чат
        VBox chatContainer = new VBox(10); // Контейнер для чата
        chatContainer.setPadding(new Insets(10));

        // Область для отображения сообщений
        chatArea = new TextArea();
        chatArea.setPrefRowCount(15);
        chatArea.setEditable(false); // Только для чтения
        chatArea.setPrefWidth(300);

        // Поле для ввода сообщений
        TextField messageField = new TextField();
        messageField.setPromptText("Введите сообщение...");
        messageField.setOnAction(event -> {
            String message = messageField.getText();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageField.clear();
            }
        });

        // Таймер
        Label timerLabel = new Label("00:30"); // Начальное значение таймера
        timerLabel.setFont(new Font("Arial", 32));

        // Добавляем элементы в контейнер чата
        chatContainer.getChildren().addAll(chatArea, messageField, timerLabel);

        // Левая часть: игроки
        GridPane playerGrid = new GridPane();
        playerGrid.setHgap(10); // Горизонтальный отступ между игроками
        playerGrid.setVgap(10); // Вертикальный отступ между игроками
        playerGrid.setPadding(new Insets(10));

        // Панель для голосования
        votePanel = new VBox(10);
        votePanel.setPadding(new Insets(10));
        voteComboBox = new ComboBox<>();
        Button voteButton = new Button("Голосовать");

        // Добавляем панель голосования в правую часть
        chatContainer.getChildren().add(votePanel);
        votePanel.getChildren().addAll(new Label("Выберите игрока:"), voteComboBox, voteButton);

        // Добавляем элементы в главный контейнер
        root.setLeft(playerGrid); // Левая часть (игроки)
        root.setRight(chatContainer); // Правая часть (чат)

        listenForMessage();

        Thread.sleep(1000);

        // Добавляем игроков в сетку
        addPlayersToGrid(playerGrid);

        // Создаем сцену и устанавливаем её на Stage
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        initializeTimer(timerLabel);

        // Настройка голосования
        setupVoting(voteComboBox, voteButton);
    }

    private void markPlayerAsDead(GridPane gridPane, String username) {
        for (Node node : gridPane.getChildren()) {
            if (node instanceof VBox) {
                VBox playerBox = (VBox) node;
                for (Node child : playerBox.getChildren()) {
                    if (child instanceof Label) {
                        Label nicknameLabel = (Label) child;
                        if (nicknameLabel.getText().equals(username)) {
                            nicknameLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            return;
                        }
                    }
                }
            }
        }
    }

    private void setupVoting(ComboBox<String> voteComboBox, Button voteButton) {
        // Обновляем список игроков для голосования
        updateVoteCandidates(voteComboBox);

        // Обработка нажатия на кнопку "Голосовать"
        voteButton.setOnAction(event -> {
            String selectedPlayer = voteComboBox.getValue();
            if (selectedPlayer != null && !selectedPlayer.isEmpty()) {
                sendMessage("/vote " + selectedPlayer);
                voteComboBox.setValue(null); // Очищаем выбор после голосования
            } else {
                Platform.runLater(() -> chatArea.appendText("SERVER: Выберите игрока для голосования.\n"));
            }
        });
    }

    // Метод для обновления списка кандидатов для голосования
    private void updateVoteCandidates(ComboBox<String> voteComboBox) {
        voteComboBox.getItems().clear(); // Очищаем текущий список
        for (String playerName : playerMap.keySet()) {
            if (!playerName.equals(username)) { // Исключаем самого игрока
                voteComboBox.getItems().add(playerName);
            }
        }
    }

    // Метод для инициализации таймера
    private void initializeTimer(Label timerLabel) {
        int[] totalTimeInSeconds = {30}; // Время одного этапа (30 секунд), используем массив для изменения значения

        timeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(1), // Интервал обновления (каждую секунду)
                        event -> {
                            if (isGameRunning && totalTimeInSeconds[0] > 0) {
                                totalTimeInSeconds[0]--; // Уменьшаем время на 1 секунду
                                String formattedTime = String.format("%02d:%02d", 0, totalTimeInSeconds[0]); // Форматируем время
                                Platform.runLater(() -> timerLabel.setText(formattedTime)); // Обновляем текст таймера
                            } else if (totalTimeInSeconds[0] == 0 && isGameRunning) {
                                // Если время истекло, но игра еще не завершена, перезапускаем таймер
                                totalTimeInSeconds[0] = 30; // Сбрасываем время обратно на 30 секунд
                                String formattedTime = String.format("%02d:%02d", 0, totalTimeInSeconds[0]); // Форматируем время
                                Platform.runLater(() -> timerLabel.setText(formattedTime)); // Обновляем текст таймера
                            } else if (!isGameRunning) {
                                stopTimer(timeline); // Останавливаем таймер при завершении игры
                            }
                        }
                )
        );
        timeline.setCycleCount(Animation.INDEFINITE); // Бесконечный цикл
        timeline.play(); // Запускаем таймер
    }

    // Метод для остановки таймера
    private void stopTimer(Timeline timeline) {
        if (timeline != null) {
            timeline.stop(); // Останавливаем таймер
        }
    }

    // Метод для добавления игроков в сетку
    private void addPlayersToGrid(GridPane gridPane) {
        // Определяем роль клиента
        String clientRole = playerMap.get(this.username);

        int row = 0; // Стартовая строка для размещения игроков
        int col = 0; // Стартовый столбец для размещения игроков

        for (Map.Entry<String, String> entry : playerMap.entrySet()) {

            // Получаем ник и роль игрока
            String username = entry.getKey();
            String role = entry.getValue();

            String avatarPath = DEFAULT_AVATAR;
            ;
            if ("Мафия".equals(clientRole)) {
                // Если клиент является Мафией
                if ("Мафия".equals(role)) {
                    avatarPath = MAFIA_AVATAR; // Все мафии получают аватарку Мафии
                } else {
                    avatarPath = PEACEFUL_AVATAR; // Остальные игроки — аватарка Мирного
                }
            } else if ("Мирный".equals(clientRole)) {
                // Если клиент является Мирным
                if (this.username.equals(username)) {
                    avatarPath = PEACEFUL_AVATAR; // Клиент получает аватарку Мирного
                } else {
                    avatarPath = DEFAULT_AVATAR; // Остальные игроки — дефолтная аватарка
                }
            }

            // Загружаем аватарку игрока
            Image avatarImage = new Image(getClass().getResourceAsStream(avatarPath));
            ImageView avatarView = new ImageView(avatarImage);
            avatarView.setFitWidth(AVATAR_SIZE); // Устанавливаем размер аватарки
            avatarView.setPreserveRatio(true); // Сохраняем пропорции

            // Создаем метку с ником игрока
            Label nicknameLabel = new Label(username);
            nicknameLabel.setFont(new Font(16));

            // Группируем аватарку и метку в VBox
            VBox playerBox = new VBox(10);
            playerBox.setAlignment(Pos.CENTER);
            playerBox.getChildren().addAll(avatarView, nicknameLabel);

            // Добавляем группу в сетку
            gridPane.add(playerBox, col, row);

            // Обновляем колонку и строку для следующего игрока
            col++;
            if (col >= GRID_COLUMNS) { // Если достигнут лимит колонок, переходим на новую строку
                col = 0;
                row++;
            }
        }
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
            if (!isDead) {
                if (!message.startsWith("/")) {
                    bufferedWriter.write(username + ": " + message);
                    Platform.runLater(() -> chatArea.appendText("You" + ": " + message + "\n"));
                } else {
                    bufferedWriter.write(message);
                }
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } else {
                Platform.runLater(() -> chatArea.appendText("SERVER: Вы мертвы и не можете отправлять сообщения."));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void listenForMessage() {
        new Thread(() -> {
            while (socket.isConnected()) {
                try {
                    String msgFromGroupChat = bufferedReader.readLine();
                    if (msgFromGroupChat == null) continue;

                    processMessage(msgFromGroupChat);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void processMessage(String msgFromGroupChat) {
        if (msgFromGroupChat.startsWith("/playerMap")) {
            String role = "";
            String username = "";
            String[] usernamesAndRoles = msgFromGroupChat.substring(11).split("-");
            for (String usernameAndRole : usernamesAndRoles) {
                String[] info = usernameAndRole.split(":");
                if (info.length == 2) {
                    username = info[0];
                    role = info[1];
                    playerMap.put(username, role);
                }
                if (username.equals(this.username)) {
                    String finalRole = role;
                    Platform.runLater(() -> chatArea.appendText("SERVER: Ваша роль - " + finalRole + "\n"));
                }
            }
        } else if ("/night".equals(msgFromGroupChat)) {
            // Фаза ночи: только Мафия может голосовать
            if ("Мафия".equals(playerMap.get(username))) {
                Platform.runLater(() -> {
                    votePanel.setVisible(true);
                    votePanel.setDisable(false);
                });
            } else {
                Platform.runLater(() -> {
                    votePanel.setVisible(false);
                    votePanel.setDisable(true);
                });
            }
        } else if ("/day".equals(msgFromGroupChat)) {
            // Фаза дня: все могут голосовать
            if (!isDead) {
                Platform.runLater(() -> {
                    votePanel.setVisible(true);
                    votePanel.setDisable(false);
                });
            } else {
                Platform.runLater(() -> {
                    votePanel.setVisible(false);
                    votePanel.setDisable(true);
                });
            }
        } else if ("/endGame".equals(msgFromGroupChat)) {
            // Останавливаем таймер при завершении игры
            isGameRunning = false;
        } else if (msgFromGroupChat.startsWith("/remove")) {
            // Игрок был удален
            String removedPlayer = msgFromGroupChat.substring(8); // Извлекаем имя удаленного игрока

            // Удаляем игрока из GridPane
            Platform.runLater(() -> markPlayerAsDead((GridPane) root.getLeft(), removedPlayer));

            // Удаляем игрока из мапы
            playerMap.remove(removedPlayer);

            if (removedPlayer.equals(username)) {
                isDead = true;
            }

            // Обновляем панель голосования
            updateVoteCandidates(voteComboBox);
        } else {
            Platform.runLater(() -> chatArea.appendText(msgFromGroupChat + "\n"));
        }
    }
}
