package org.example;

import org.example.apiserver.Server;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(8080, 0);

        server.start();
    }
}
