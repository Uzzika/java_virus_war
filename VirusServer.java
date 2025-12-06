package org.example;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class VirusServer {
    public static void main(String[] args) {
        try {
            System.out.println("Запуск сервера Virus Game...");
            LocateRegistry.createRegistry(1099);
            GameService service = new GameServiceImpl();
            Naming.rebind("VirusGame", service);
            System.out.println("✅ Сервер успешно запущен!");
            System.out.println("✅ Реестр RMI работает на порту 1099");
            System.out.println("✅ Игровой сервис привязан как 'VirusGame'");
            System.out.println("\nОжидание подключения игроков...");
        } catch (Exception e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
            e.printStackTrace();
        }
    }
}