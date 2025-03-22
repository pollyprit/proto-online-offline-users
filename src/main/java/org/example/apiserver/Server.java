package org.example.apiserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

public class Server implements HttpHandler {
    private int PORT = 8080;
    private int BACKLOG = 0;
    private HttpServer server;

    private String GET_STATUS_URI = "/api/status";
    private String POST_HEARTBEAT_URI = "/api/heartbeat";


    public Server(int port, int backlog) {
        this.PORT = port;
        this.BACKLOG = backlog;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), BACKLOG);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        server.createContext(GET_STATUS_URI, this);
        server.createContext(POST_HEARTBEAT_URI, this);

        server.setExecutor(null);  // use default executor

        System.out.println("Server starting on PORT " + this.PORT);

        server.start();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String httpMethod = exchange.getRequestMethod();

        switch (httpMethod) {
            case "GET":
                handleGetMethod(exchange);
                break;

            case "POST":
                handlePostMethod(exchange);
                break;

            default:
                exchange.sendResponseHeaders(405, 0);
                break;
        }
    }

    private void handleGetMethod(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();

        if (GET_STATUS_URI.equals(uri.getPath())) {
            String response = "Hello GET";

            exchange.sendResponseHeaders(200, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
        }
    }

    private void handlePostMethod(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();

        if (POST_HEARTBEAT_URI.equals(uri.getPath())) {
            String response = "Hello POST";

            exchange.sendResponseHeaders(200, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
        }
    }
}
