package org.example;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Scanner;

public class VirusClient {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        try {
            GameService service = (GameService) Naming.lookup("rmi://localhost/VirusGame");
            System.out.println("Добро пожаловать в игру \"Война вирусов\".");

            System.out.print("Введите ваше имя игрока: ");
            String id = sc.nextLine().trim();
            if (id.isEmpty()) {
                System.out.println("Имя не может быть пустым. Завершение.");
                return;
            }

            char me = service.join(id);
            if (me == 0) {
                System.out.println("Сервер переполнен или подключение не удалось.");
                return;
            }

            System.out.println("Вы подключились как игрок '" + me + "'.");
            if (me == 'X') {
                System.out.println("Вы играете крестиками. Крестики ходят первыми.");
            } else {
                System.out.println("Вы играете ноликами. Крестики начинают первыми.");
            }

            // Ждём, пока игра реально стартует (подключатся оба игрока)
            if (!service.isGameStarted()) {
                System.out.println("Ожидаем подключения второго игрока...");
                while (!service.isGameStarted()) {
                    System.out.print("Нажмите Enter, чтобы проверить статус (или введите 'exit' для выхода): ");
                    String line = sc.nextLine().trim();
                    if (line.equalsIgnoreCase("exit")) {
                        System.out.println("Выход из клиента.");
                        return;
                    }
                    if (service.isGameStarted()) {
                        System.out.println("Второй игрок подключился. Игра началась!");
                        break;
                    } else {
                        System.out.println("Второй игрок ещё не подключился.");
                    }
                }
            }

            // Основной игровой цикл
            while (true) {
                if (service.isGameOver()) {
                    printBoard(service.getBoard());
                    char winner = service.checkWinner();
                    if (winner == 'D') {
                        System.out.println("Игра окончена. Ничья.");
                    } else if (winner == me) {
                        System.out.println("Игра окончена. Вы победили!");
                    } else {
                        System.out.println("Игра окончена. Победил игрок '" + winner + "'. Вы проиграли.");
                    }
                    break;
                }

                char current = service.getCurrentPlayer();
                char[][] board = service.getBoard();
                printBoard(board);

                if (current != me) {
                    System.out.println("Сейчас ходит игрок '" + current + "'.");
                    System.out.print("Нажмите Enter, чтобы обновить состояние или введите 'exit' для выхода: ");
                    String line = sc.nextLine().trim();
                    if (line.equalsIgnoreCase("exit")) {
                        System.out.println("Выход из клиента.");
                        break;
                    }
                    // просто обновляем цикл
                    continue;
                }

                // Наш ход
                int left;
                try {
                    left = service.getActionsLeft(id);
                } catch (RemoteException e) {
                    System.out.println("Ошибка при запросе оставшихся \"ходиков\": " + e.getMessage());
                    break;
                }

                System.out.println("Ваш ход (" + me + "). Осталось \"ходиков\" в этом ходу: " + left + " из 3.");
                printMenu();

                System.out.print("Выберите действие: ");
                String choice = sc.nextLine().trim();

                if (choice.equalsIgnoreCase("0") || choice.equalsIgnoreCase("exit")) {
                    System.out.println("Выход из клиента.");
                    break;
                } else if (choice.equals("1")) {
                    // Размножение (PLACE)
                    Action action = readAction(sc, Action.Type.PLACE);
                    if (action == null) {
                        // пользователь отменил
                        continue;
                    }
                    boolean ok = service.makeAction(id, action);
                    if (!ok) {
                        System.out.println("Ход невозможен: либо клетка занята/недоступна, либо нарушены правила.");
                    } else {
                        System.out.println("Фишка успешно поставлена.");
                    }

                } else if (choice.equals("2")) {
                    // Убийство (KILL)
                    Action action = readAction(sc, Action.Type.KILL);
                    if (action == null) {
                        continue;
                    }
                    boolean ok = service.makeAction(id, action);
                    if (!ok) {
                        System.out.println("Убить здесь нельзя: либо здесь нет живой вражеской фишки, либо клетка недоступна.");
                    } else {
                        System.out.println("Фишка противника убита.");
                    }

                } else if (choice.equals("3")) {
                    // Пас
                    boolean ok = service.passTurn(id);
                    if (!ok) {
                        System.out.println("Пас невозможен сейчас.");
                        System.out.println("Причины могут быть такие:");
                        System.out.println(" - ещё не ваша очередь;");
                        System.out.println(" - у вас есть возможные ходы, но вы уже сделали 1–2 \"ходика\" (по правилам нельзя завершить ход раньше трёх);");
                        System.out.println(" - ход уже завершён.");
                    } else {
                        System.out.println("Вы пропустили ход (или завершили его).");
                    }

                } else if (choice.equals("4") || choice.equalsIgnoreCase("r")) {
                    // Просто обновить доску
                    System.out.println("Обновление доски...");
                    // ничего не делаем, цикл заново покажет состояние
                } else {
                    System.out.println("Неизвестная команда. Введите номер из меню.");
                }
            }

        } catch (Exception e) {
            System.out.println("Ошибка при работе с сервером: " + e.getMessage());
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }

    /**
     * Прочитать координаты для действия PLACE/KILL.
     * Возвращает null, если пользователь отменил ввод.
     */
    private static Action readAction(Scanner sc, Action.Type type) {
        while (true) {
            System.out.print("Введите координаты (строка и столбец от 0 до 9 через пробел, или 'c' для отмены): ");
            String line = sc.nextLine().trim();
            if (line.equalsIgnoreCase("c") || line.equalsIgnoreCase("cancel")) {
                System.out.println("Действие отменено.");
                return null;
            }
            String[] parts = line.split("\\s+");
            if (parts.length != 2) {
                System.out.println("Ожидалось два числа: строка и столбец.");
                continue;
            }
            try {
                int r = Integer.parseInt(parts[0]);
                int c = Integer.parseInt(parts[1]);
                if (r < 0 || r > 9 || c < 0 || c > 9) {
                    System.out.println("Координаты должны быть от 0 до 9.");
                    continue;
                }
                return new Action(type, r, c);
            } catch (NumberFormatException ex) {
                System.out.println("Нужно вводить числа. Пример: 3 4");
            }
        }
    }

    private static void printMenu() {
        System.out.println("Доступные действия:");
        System.out.println(" 1 — Размножение (поставить свою фишку)");
        System.out.println(" 2 — Убить фишку противника");
        System.out.println(" 3 — Пас (пропустить ход / завершить ход)");
        System.out.println(" 4 — Обновить доску");
        System.out.println(" 0 — Выход из игры (только клиент, сервер продолжит игру для второго игрока).");
    }

    private static void printBoard(char[][] b) {
        System.out.println();
        System.out.println("     0 1 2 3 4 5 6 7 8 9");
        System.out.println("   ┌─────────────────────┐");
        for (int i = 0; i < b.length; i++) {
            System.out.print(i + "  │ ");
            for (int j = 0; j < b[i].length; j++) {
                char c = b[i][j];
                if (c == 'X' || c == 'O' || c == 'x' || c == 'o')
                    System.out.print(c + " ");
                else
                    System.out.print(". ");
            }
            System.out.println("│");
        }
        System.out.println("   └─────────────────────┘");
        System.out.println("Легенда: X/O — живые, x/o — убитые. Координаты вводятся как: строка столбец (0–9).");
        System.out.println();
    }

}
