package ru.itis.mafia;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class MafiaServer {
    private static List<Handler> clientHandlers = new ArrayList<>();
    private static final int PORT = 1234;
    private static final int MAX_PLAYERS = 4;
    private static List<Handler> mafiaPlayer = new ArrayList<>();
    private static List<Handler> peacefulPlayer = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер стартанул");
            while (clientHandlers.size() < MAX_PLAYERS) {
                Socket socket = serverSocket.accept();
                System.out.println("Клиент подключился");
                Handler chatHandler = new Handler(socket);
                Thread thread = new Thread(chatHandler);
                thread.start();
                clientHandlers.add(chatHandler);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Handler.serverMessage("/startGame");
        assignRoles();
        Handler.sendPlayerMap();
        startGame();
    }

    private static void assignRoles() {
        Collections.shuffle(clientHandlers);
        int mafiaCount = 1;
        for (int i = 0; i < clientHandlers.size(); i++) {
            if (i < mafiaCount) {
                clientHandlers.get(i).setRole("Мафия");
                mafiaPlayer.add(clientHandlers.get(i));
            } else {
                clientHandlers.get(i).setRole("Мирный");
                peacefulPlayer.add(clientHandlers.get(i));
            }
        }
    }

    private static void startGame() {
        while (!mafiaPlayer.isEmpty() && peacefulPlayer.size() != mafiaPlayer.size()) {
            // Фаза ночи
            Handler.serverMessage("/night");
            Handler.serverMessage("SERVER: Ночь! Мафия выбирает жертву.");
            for (Handler client : clientHandlers) {
                client.setDay(false);
            }

            try {
                Thread.sleep(31000); // Время на голосование (30 секунд)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String victim = voteForKill(mafiaPlayer, peacefulPlayer);
            if (victim != null) {
                Handler.serverMessage("/remove " + victim);
                Handler.serverMessage("SERVER: Мафия убила " + victim);
                removePlayer(victim);
            }

            if (peacefulPlayer.size() == mafiaPlayer.size()) {
                break;
            }

            // Фаза дня
            Handler.serverMessage("/day");
            Handler.serverMessage("SERVER: День! Мирные голосуют.");
            for (Handler client : clientHandlers) {
                client.setDay(true);
            }

            try {
                Thread.sleep(31000); // Время на голосование (30 секунд)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String votedOut = voteForKill(clientHandlers, clientHandlers);
            if (votedOut != null) {
                Handler.serverMessage("/remove " + votedOut); // Отправляем команду для удаления игрока
                Handler.serverMessage("SERVER: Игрок " + votedOut + " был изгнан!");
                removePlayer(votedOut);
            }
        }
        Handler.serverMessage("/endGame");
        if (mafiaPlayer.isEmpty()) {
            Handler.serverMessage("SERVER: Мирные победили!");
        } else {
            Handler.serverMessage("SERVER: Мафия победила!");
        }
    }

    private static String voteForKill(List<Handler> voters, List<Handler> victims) {
        Map<String, Integer> votes = new HashMap<>();
        Random random = new Random();

        for (Handler voter : voters) {
            String votedPlayer = voter.getVotePlayer();
            if (votedPlayer == null) {
                votedPlayer = victims.get(random.nextInt(victims.size())).getUsername();
            }
            votes.put(votedPlayer, votes.getOrDefault(votedPlayer, 0) + 1);
            voter.setVotePlayer(null);
        }

        if (votes.isEmpty()) return null;

        int maxVotes = Collections.max(votes.values());
        List<String> topVoted = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : votes.entrySet()) {
            if (entry.getValue() == maxVotes) {
                topVoted.add(entry.getKey());
            }
        }

        return topVoted.get(random.nextInt(topVoted.size())); // Если ничья — выбираем случайно
    }

    private static void removePlayer(String playerName) {
        clientHandlers.removeIf(handler -> handler.getUsername().equals(playerName));
        mafiaPlayer.removeIf(handler -> handler.getUsername().equals(playerName));
        peacefulPlayer.removeIf(handler -> handler.getUsername().equals(playerName));
    }
}
