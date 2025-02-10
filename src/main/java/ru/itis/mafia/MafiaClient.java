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
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.Socket;
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

    private static final double AVATAR_SIZE = 100;
    private static final String DEFAULT_AVATAR = "/Игрок.png";
    private static final String MAFIA_AVATAR = "/Мафия.png";
    private static final String PEACEFUL_AVATAR = "/Мирный.png";
    private static final int MAX_PLAYERS = 4;
    private static final int GRID_COLUMNS = MAX_PLAYERS / 2;

    private String username;
    private boolean isDead = false;
    private boolean isGameRunning = true;

    public MafiaClient(Socket socket) {
        try {
            this.socket = socket;
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            username = "Игрок " + new Random().nextInt(1000);

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
        primaryStage.setTitle("Игра Мафия - " + username);

        root = new BorderPane();
        root.setPadding(new Insets(10));

        VBox chatContainer = new VBox(10);
        chatContainer.setPadding(new Insets(10));

        chatArea = new TextArea();
        chatArea.setPrefRowCount(15);
        chatArea.setEditable(false);
        chatArea.setPrefWidth(300);

        TextField messageField = new TextField();
        messageField.setPromptText("Введите сообщение...");
        messageField.setOnAction(event -> {
            String message = messageField.getText();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageField.clear();
            }
        });

        Label timerLabel = new Label("00:30");
        timerLabel.setFont(new Font("Arial", 32));

        chatContainer.getChildren().addAll(chatArea, messageField, timerLabel);

        GridPane playerGrid = new GridPane();
        playerGrid.setHgap(10);
        playerGrid.setVgap(10);
        playerGrid.setPadding(new Insets(10));

        votePanel = new VBox(10);
        votePanel.setPadding(new Insets(10));
        voteComboBox = new ComboBox<>();
        Button voteButton = new Button("Голосовать");

        chatContainer.getChildren().add(votePanel);
        votePanel.getChildren().addAll(new Label("Выберите игрока:"), voteComboBox, voteButton);

        root.setLeft(playerGrid);
        root.setRight(chatContainer);

        listenForMessage();

        Thread.sleep(1000);

        addPlayersToGrid(playerGrid);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        initializeTimer(timerLabel);

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
        updateVoteCandidates(voteComboBox);

        voteButton.setOnAction(event -> {
            String selectedPlayer = voteComboBox.getValue();
            if (selectedPlayer != null && !selectedPlayer.isEmpty()) {
                sendMessage("/vote " + selectedPlayer);
                voteComboBox.setValue(null);
            } else {
                Platform.runLater(() -> chatArea.appendText("SERVER: Выберите игрока для голосования.\n"));
            }
        });
    }

    private void updateVoteCandidates(ComboBox<String> voteComboBox) {
        voteComboBox.getItems().clear();
        for (String playerName : playerMap.keySet()) {
            if (!playerName.equals(username)) {
                voteComboBox.getItems().add(playerName);
            }
        }
    }

    private void initializeTimer(Label timerLabel) {
        int[] totalTimeInSeconds = {30};

        timeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        event -> {
                            if (isGameRunning && totalTimeInSeconds[0] > 0) {
                                totalTimeInSeconds[0]--;
                                String formattedTime = String.format("%02d:%02d", 0, totalTimeInSeconds[0]);
                                Platform.runLater(() -> timerLabel.setText(formattedTime));
                            } else if (totalTimeInSeconds[0] == 0 && isGameRunning) {
                                totalTimeInSeconds[0] = 30;
                                String formattedTime = String.format("%02d:%02d", 0, totalTimeInSeconds[0]);
                                Platform.runLater(() -> timerLabel.setText(formattedTime));
                            } else if (!isGameRunning) {
                                stopTimer(timeline);
                            }
                        }
                )
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void stopTimer(Timeline timeline) {
        if (timeline != null) {
            timeline.stop();
        }
    }

    private void addPlayersToGrid(GridPane gridPane) {
        String clientRole = playerMap.get(this.username);

        int row = 0;
        int col = 0;

        for (Map.Entry<String, String> entry : playerMap.entrySet()) {

            String username = entry.getKey();
            String role = entry.getValue();

            String avatarPath = DEFAULT_AVATAR;
            if ("Мафия".equals(clientRole)) {
                if ("Мафия".equals(role)) {
                    avatarPath = MAFIA_AVATAR;
                } else {
                    avatarPath = PEACEFUL_AVATAR;
                }
            } else if ("Мирный".equals(clientRole)) {
                if (this.username.equals(username)) {
                    avatarPath = PEACEFUL_AVATAR;
                } else {
                    avatarPath = DEFAULT_AVATAR;
                }
            }

            Image avatarImage = new Image(getClass().getResourceAsStream(avatarPath));
            ImageView avatarView = new ImageView(avatarImage);
            avatarView.setFitWidth(AVATAR_SIZE);
            avatarView.setPreserveRatio(true);

            Label nicknameLabel = new Label(username);
            nicknameLabel.setFont(new Font(16));

            VBox playerBox = new VBox(10);
            playerBox.setAlignment(Pos.CENTER);
            playerBox.getChildren().addAll(avatarView, nicknameLabel);

            gridPane.add(playerBox, col, row);

            col++;
            if (col >= GRID_COLUMNS) {
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
            isGameRunning = false;
        } else if (msgFromGroupChat.startsWith("/remove")) {
            String removedPlayer = msgFromGroupChat.substring(8); // Извлекаем имя удаленного игрока

            Platform.runLater(() -> markPlayerAsDead((GridPane) root.getLeft(), removedPlayer));

            playerMap.remove(removedPlayer);

            if (removedPlayer.equals(username)) {
                isDead = true;
            }

            updateVoteCandidates(voteComboBox);
        } else {
            Platform.runLater(() -> chatArea.appendText(msgFromGroupChat + "\n"));
        }
    }
}
