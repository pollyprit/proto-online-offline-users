package org.example;

import org.example.apiserver.Server;
import org.example.apiserver.StorageStrategy;

public class Main {
    public static int PORT = 8080;

    public static void main(String[] args) {
        Server server = new Server(PORT, 0, StorageStrategy.REDIS);

        server.start();
    }
}
