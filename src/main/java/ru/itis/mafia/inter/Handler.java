package ru.itis.mafia.inter;

public interface Handler extends Runnable {
    void kill(String clientId);
    void vote(String senderId, String receivedId);
}
