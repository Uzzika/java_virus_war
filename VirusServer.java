package org.example;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class VirusServer {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099); // запускаем RMI Registry
            GameService service = new GameServiceImpl();

            Naming.rebind("VirusGame", service); // <<< имя должно совпадать с клиентом

            System.out.println("Server started. RMI object bound as 'VirusGame'");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}