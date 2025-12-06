package org.example;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GameServiceImpl extends UnicastRemoteObject implements GameService {

    private static final int SIZE = 10;
    private static final int ACTIONS_PER_TURN = 3;

    // '.' — пусто, 'X'/'O' — живые, 'x'/'o' — убитые
    private final char[][] board = new char[SIZE][SIZE];

    private String playerX = null;
    private String playerO = null;

    // чей сейчас ход: 'X' или 'O'
    private char currentPlayer = 'X';

    // сколько "ходиков" осталось у игрока в текущем ходу
    private final Map<String, Integer> actionsLeft = new HashMap<>();

    // был ли пас вынужденным (нет возможных ходов)
    private final Map<String, Boolean> forcedPass = new HashMap<>();

    private boolean gameStarted = false;
    private boolean gameOver = false;
    // 'X', 'O', 'D' (ничья) или '.' если ещё не определён
    private char gameWinner = '.';

    public GameServiceImpl() throws RemoteException {
        super();
        for (int i = 0; i < SIZE; i++) {
            Arrays.fill(board[i], '.');
        }
    }

    @Override
    public synchronized char join(String playerId) throws RemoteException {
        // первый подключившийся — X
        if (playerX == null) {
            playerX = playerId;
            actionsLeft.put(playerId, 0);       // до старта игры ходов нет
            forcedPass.put(playerId, false);
            System.out.println("Player joined as X: " + playerId);
            return 'X';
        }

        // второй — O, после этого игра стартует, ходят крестики
        if (playerO == null) {
            playerO = playerId;
            forcedPass.put(playerId, false);
            gameStarted = true;
            currentPlayer = 'X';
            // X получает первый ход: 3 "ходика"
            if (playerX != null) {
                actionsLeft.put(playerX, ACTIONS_PER_TURN);
            }
            actionsLeft.put(playerO, 0);
            System.out.println("Player joined as O: " + playerId + ". Game started.");
            return 'O';
        }

        // Больше двух игроков не принимаем
        return 0;
    }

    @Override
    public synchronized boolean makeAction(String playerId, Action action) throws RemoteException {
        if (!gameStarted || gameOver) return false;

        // ход может делать только текущий игрок
        if (!playerId.equals(currentPlayerId())) return false;

        char symbol = playerIdToMark(playerId);
        if (symbol == 0) return false;

        int left = actionsLeft.getOrDefault(playerId, 0);
        if (left <= 0) return false; // ходики закончились

        int r = action.row;
        int c = action.col;

        if (!inBounds(r, c)) return false;

        char enemy = opponentOf(symbol);

        switch (action.type) {
            case PLACE:
                // можно ставить только в пустую и ДОСТУПНУЮ клетку
                if (board[r][c] != '.') return false;
                if (!isCellAccessibleForPlace(r, c, symbol)) return false;
                board[r][c] = symbol;
                break;

            case KILL:
                // можно убивать только живой символ противника на ДОСТУПНОЙ клетке
                if (board[r][c] != enemy) return false;
                if (!isCellAccessibleForPlace(r, c, symbol)) return false;
                board[r][c] = Character.toLowerCase(enemy); // убитый
                break;

            default:
                return false;
        }
        // игрок сделал реальное действие — это точно не пас
        forcedPass.put(playerId, false);

        // уменьшаем число оставшихся ходиков
        actionsLeft.put(playerId, left - 1);

        // после любого действия проверяем победителя по состоянию доски
        char w = checkWinner();
        if (w != '.') {
            // кто-то выиграл или зафиксировалась ничья по уничтожению обоих
            gameOver = true;
            gameWinner = w;
            return true;
        }

        // если ходики кончились — конец хода
        checkEndTurn(playerId);
        return true;
    }

    @Override
    public synchronized boolean passTurn(String playerId) throws RemoteException {
        if (!gameStarted || gameOver) return false;

        if (!playerId.equals(currentPlayerId())) return false;

        char symbol = playerIdToMark(playerId);
        if (symbol == 0) return false;

        int left = actionsLeft.getOrDefault(playerId, 0);

        // уже сделаны 1–2 "ходика", пасить можно только если ходов больше нет
        if (left < ACTIONS_PER_TURN && left > 0) {
            if (canPlayerMakeAnyAction(symbol)) {
                // есть возможные действия, нельзя завершать ход раньше 3
                return false;
            }
            // продолжать ход невозможно
            forcedPass.put(playerId, true);

        } else if (left == ACTIONS_PER_TURN) {
            // игрок ещё не сделал ни одного "ходика" в этом ходу
            if (canPlayerMakeAnyAction(symbol)) {
                // можно ходить, но игрок отказывается
                forcedPass.put(playerId, false);
            } else {
                // даже первый "ходик" сделать невозможно
                forcedPass.put(playerId, true);
            }

        } else if (left == 0) {
            // ход уже закончился, пасить нельзя
            return false;
        }

        // игрок больше не ходит в этом ходу
        actionsLeft.put(playerId, 0);

        // если оба игрока пасовали — ничья
        if (bothPlayersForcedPass()) {
            gameOver = true;
            gameWinner = 'D';
            return true;
        }

        endTurn();
        return true;
    }

    @Override
    public synchronized char[][] getBoard() throws RemoteException {
        char[][] copy = new char[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, SIZE);
        }
        return copy;
    }

    @Override
    public synchronized char getCurrentPlayer() throws RemoteException {
        return currentPlayer;
    }

    @Override
    public synchronized char checkWinner() throws RemoteException {
        if (!gameStarted) return '.';

        // ничья по двойному пасу
        if (gameOver && gameWinner == 'D') {
            return 'D';
        }

        int aliveX = 0, aliveO = 0;
        int deadX = 0, deadO = 0;

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                char cell = board[r][c];
                if (cell == 'X') aliveX++;
                else if (cell == 'O') aliveO++;
                else if (cell == 'x') deadX++;
                else if (cell == 'o') deadO++;
            }
        }

        // X победил: у O нет живых, но есть хотя бы одна убитая фишка O
        boolean oDestroyed = (aliveO == 0 && deadO > 0);
        // O победил: у X нет живых, но есть хотя бы одна убитая фишка X
        boolean xDestroyed = (aliveX == 0 && deadX > 0);

        if (oDestroyed && !xDestroyed) return 'X';
        if (xDestroyed && !oDestroyed) return 'O';

        // обе колонии уничтожены можно трактовать как ничью
        if (xDestroyed && oDestroyed) return 'D';

        // игра продолжается
        return '.';
    }

    @Override
    public synchronized boolean isGameOver() throws RemoteException {
        // игра окончена, если уже есть результат
        // или видно, что у кого-то не осталось живых
        char w = checkWinner();
        return w != '.';
    }

    @Override
    public synchronized int getActionsLeft(String playerId) throws RemoteException {
        return actionsLeft.getOrDefault(playerId, 0);
    }

    @Override
    public synchronized boolean isGameStarted() throws RemoteException {
        return gameStarted;
    }

    private String currentPlayerId() {
        return (currentPlayer == 'X') ? playerX : playerO;
    }

    private char playerIdToMark(String playerId) {
        if (playerId == null) return 0;
        if (playerId.equals(playerX)) return 'X';
        if (playerId.equals(playerO)) return 'O';
        return 0;
    }

    private char opponentOf(char symbol) {
        return (symbol == 'X') ? 'O' : 'X';
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < SIZE && c >= 0 && c < SIZE;
    }

    private boolean isBoardEmpty() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] != '.') return false;
            }
        }
        return true;
    }

    private boolean hasAlive(char symbol) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == symbol) return true;
            }
        }
        return false;
    }

    // есть ли у игрока хотя бы одно возможное действие (PLACE или KILL)
    private boolean canPlayerMakeAnyAction(char symbol) {
        char enemy = opponentOf(symbol);

        // если у игрока нет живых фишек,
        // он всегда может поставить первую на свою стартовую клетку, если она свободна
        if (!hasAlive(symbol)) {
            if (symbol == 'X' && board[0][0] == '.') return true;
            if (symbol == 'O' && board[SIZE - 1][SIZE - 1] == '.') return true;
        }

        // любая доступная пустая клетка для PLACE
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == '.' && isCellAccessibleForPlace(i, j, symbol)) {
                    return true;
                }
            }
        }

        // любая доступная вражеская живая клетка для KILL
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == enemy && isCellAccessibleForPlace(i, j, symbol)) {
                    return true;
                }
            }
        }

        return false;
    }

    // после трёх ходиков
    private void checkEndTurn(String playerId) {
        int left = actionsLeft.getOrDefault(playerId, 0);
        if (left <= 0) {
            endTurn();
        }
    }

    // переключение хода между X и O
    private void endTurn() {
        if (gameOver) return;

        currentPlayer = opponentOf(currentPlayer);
        String nextId = currentPlayerId();
        if (nextId != null) {
            actionsLeft.put(nextId, ACTIONS_PER_TURN);
        }
    }

    // оба игрока пасовали (нет возможных ходов)
    private boolean bothPlayersForcedPass() {
        if (playerX == null || playerO == null) return false;
        Boolean fx = forcedPass.get(playerX);
        Boolean fo = forcedPass.get(playerO);
        return Boolean.TRUE.equals(fx) && Boolean.TRUE.equals(fo);
    }

    /**
     * Клетка доступна для символа symbol ('X' или 'O'), если:
     * 1) это стартовое исключение (первая фишка X на (0,0), O на (9,9) при пустой доске), или
     * 2) она соседствует (8 направлений) с живым своим символом, или
     * 3) до неё можно добраться через цепочку убитых вражеских фишек.
     */
    private boolean isCellAccessibleForPlace(int row, int col, char symbol) {
        if (!inBounds(row, col)) return false;

        // Старт первая фишка игрока
        if (!hasAlive(symbol)) {
            if (symbol == 'X' && row == 0 && col == 0 && board[0][0] == '.') return true;
            if (symbol == 'O' && row == SIZE - 1 && col == SIZE - 1 && board[SIZE - 1][SIZE - 1] == '.') return true;
        }

        // соприкосновение с живой своей фишкой
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = row + dr;
                int nc = col + dc;
                if (!inBounds(nr, nc)) continue;
                if (board[nr][nc] == symbol) return true;
            }
        }

        // достижимость через цепочку убитых вражеских
        boolean[][] visited = new boolean[SIZE][SIZE];
        return accessibleThroughKilledChain(row, col, symbol, visited);
    }

    private boolean accessibleThroughKilledChain(int row, int col, char symbol, boolean[][] visited) {
        if (!inBounds(row, col) || visited[row][col]) return false;
        visited[row][col] = true;

        char enemy = opponentOf(symbol);
        char deadEnemy = Character.toLowerCase(enemy);

        // Если рядом живой свой символ — доступно
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = row + dr;
                int nc = col + dc;
                if (!inBounds(nr, nc)) continue;
                if (board[nr][nc] == symbol) return true;
            }
        }

        // идём дальше по убитым вражеским
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = row + dr;
                int nc = col + dc;
                if (!inBounds(nr, nc)) continue;
                if (board[nr][nc] == deadEnemy && !visited[nr][nc]) {
                    if (accessibleThroughKilledChain(nr, nc, symbol, visited)) return true;
                }
            }
        }

        return false;
    }
}
