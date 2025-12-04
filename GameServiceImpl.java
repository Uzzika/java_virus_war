package org.example;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class GameServiceImpl extends UnicastRemoteObject implements GameService {

    private static final int SIZE = 3;
    private final char[][] board = new char[SIZE][SIZE];
    private final Map<String, Character> players = new HashMap<>();
    private char currentTurn = 'X';

    public GameServiceImpl() throws RemoteException {
        super();

        // заполнить поле точками
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++)
                board[i][j] = '.';
    }

    @Override
    public synchronized char join(String playerId) throws RemoteException {
        if (players.size() >= 2) return 0; // места нет

        char sym = players.isEmpty() ? 'X' : 'O';
        players.put(playerId, sym);
        return sym;
    }

    @Override
    public synchronized boolean makeMove(String playerId, int r, int c) throws RemoteException {
        if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) return false;

        char symbol = players.get(playerId);
        if (symbol != currentTurn) return false;       // не его ход
        if (board[r][c] != '.') return false;          // занято

        board[r][c] = symbol;

        // смена хода
        currentTurn = (currentTurn == 'X' ? 'O' : 'X');
        return true;
    }

    @Override
    public synchronized char[][] getBoard() throws RemoteException {
        return board;
    }

    @Override
    public synchronized char currentTurn() throws RemoteException {
        return currentTurn;
    }

    @Override
    public synchronized char checkWinner() throws RemoteException {
        // строки
        for (int i = 0; i < SIZE; i++) {
            if (board[i][0] != '.' && board[i][0] == board[i][1] && board[i][1] == board[i][2])
                return board[i][0];
        }
        // колонки
        for (int i = 0; i < SIZE; i++) {
            if (board[0][i] != '.' && board[0][i] == board[1][i] && board[1][i] == board[2][i])
                return board[0][i];
        }
        // диагонали
        if (board[0][0] != '.' && board[0][0] == board[1][1] && board[1][1] == board[2][2])
            return board[0][0];
        if (board[0][2] != '.' && board[0][2] == board[1][1] && board[1][1] == board[2][0])
            return board[0][2];

        return '.'; // победителя нет
    }
}