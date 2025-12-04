package org.example;

import java.rmi.Naming;
import java.util.Scanner;

public class VirusClient {
    public static void main(String[] args) {
        try {
            GameService service = (GameService) Naming.lookup("rmi://localhost/VirusGame");
            Scanner sc = new Scanner(System.in);

            System.out.print("Enter your playerId: ");
            String id = sc.nextLine();

            char my = service.join(id);
            if (my == 0) {
                System.out.println("Game already full!");
                return;
            }
            System.out.println("Joined as: " + my);

            while (true) {
                printBoard(service.getBoard());
                System.out.println("Current turn: " + service.currentTurn());

                if (service.currentTurn() != my) {
                    Thread.sleep(1500);
                    continue;
                }

                System.out.print("Row col: ");
                int r = sc.nextInt();
                int c = sc.nextInt();
                boolean ok = service.makeMove(id, r, c);
                System.out.println(ok ? "Move OK" : "Bad move");

                char w = service.checkWinner();
                if (w != '.') {
                    printBoard(service.getBoard());
                    System.out.println("WINNER = " + w);
                    break;
                }
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void printBoard(char[][] b) {
        for (char[] row : b) {
            for (char c : row) System.out.print(c + " ");
            System.out.println();
        }
        System.out.println();
    }
}