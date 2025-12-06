package org.example;

import java.rmi.Naming;
import java.util.Scanner;

public class VirusClient {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        try {
            GameService service = (GameService) Naming.lookup("rmi://localhost/VirusGame");
            System.out.print("Введите ваше имя игрока: ");
            String id = sc.nextLine().trim();
            char me = service.join(id);
            if (me == 0) {
                System.out.println("Сервер переполнен или подключение не удалось.");
                return;
            }
            System.out.println("Вы присоединились как: " + me + " ");

            while (true) {
                char[][] board = service.getBoard();
                printBoard(board);

                if (service.isGameOver()) {
                    char w = service.checkWinner();
                    if (w == 'D') System.out.println("Игра закончилась НИЧЬЕЙ.");
                    else System.out.println("Игра окончена! Победитель: " + w);
                    break;
                }

                char current = service.getCurrentPlayer();
                if (current != me) {
                    System.out.println("Жду оппонента... (опрос)");
                            Thread.sleep(1500);
                    continue;
                }

                int left = service.getActionsLeft(id);
                System.out.println("Ваш ход (" + left + " действия влево). Выберите действие:");
                System.out.println("1) PLACE (разместить)");
                System.out.println("2) KILL  (убить)");
                System.out.println("3) PASS  (пропустить ход)");
                System.out.println("4) REFRESH");
                System.out.print("> ");
                String choice = sc.nextLine().trim();

                if (choice.equals("4")) continue;

                if (choice.equals("3")) {
                    boolean ok = service.passTurn(id);
                    if (!ok) System.out.println("Пропуск отклонен сервером (выполнены частичные действия, хотя полный ход возможен).");
                    continue;
                }

                System.out.print("Введите строку (0-9):");
                int r = Integer.parseInt(sc.nextLine().trim());
                System.out.print("Введите столбец (0-9):");
                int c = Integer.parseInt(sc.nextLine().trim());

                Action.Type t = choice.equals("1") ? Action.Type.PLACE : Action.Type.KILL;
                Action a = new Action(t, r, c);
                boolean res = service.makeAction(id, a);
                if (!res) System.out.println("Действие отклонено сервером (неверный ход или недоступная ячейка).");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }

    private static void printBoard(char[][] b) {
        System.out.println("    A B C D E F G H I J");
        System.out.println("   ┌───────────────────┐");
        for (int i = 0; i < b.length; i++) {
            System.out.print((i + 1) + (i < 9 ? "  │ " : " │ "));
            for (int j = 0; j < b[i].length; j++) {
                char c = b[i][j];
                if (c == 'X') System.out.print("X ");
                else if (c == 'O') System.out.print("O ");
                else if (c == 'x') System.out.print("x ");
                else if (c == 'o') System.out.print("o ");
                else System.out.print(". ");
            }
            System.out.println("│");
        }
        System.out.println("   └───────────────────┘");
        System.out.println("Легенда: заглавные буквы = жив, строчные буквы = убит. Специальные предложения: A1(0,0) - X может начинаться здесь; J10(9,9) - O может начинаться здесь.");
    }
}
