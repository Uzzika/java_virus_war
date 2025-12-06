package org.example;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameService extends Remote {
    char join(String playerId) throws RemoteException;
    boolean makeAction(String playerId, Action action) throws RemoteException;
    boolean passTurn(String playerId) throws RemoteException;
    char[][] getBoard() throws RemoteException;
    char getCurrentPlayer() throws RemoteException;
    char checkWinner() throws RemoteException;
    boolean isGameOver() throws RemoteException;
    int getActionsLeft(String playerId) throws RemoteException;
    boolean isGameStarted() throws RemoteException;
}
