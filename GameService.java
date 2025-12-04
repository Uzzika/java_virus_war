package org.example;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameService extends Remote {
    char join(String playerId) throws RemoteException;
    boolean makeMove(String playerId, int r, int c) throws RemoteException; // <-- добавили playerId
    char[][] getBoard() throws RemoteException;
    char currentTurn() throws RemoteException;
    char checkWinner() throws RemoteException;
}