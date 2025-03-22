package org.example.apiserver;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.example.database.DBConnectionPool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Database: test
 * Table: user_heartbeats(
 * 	        id SERIAL PRIMARY KEY,
 * 	        last_hb TIMESTAMP);
 *
 * 	 HTTP Methods:
 * 	 1) POST /api/heartbeat    [updates the heartbeat for user to current timestamp]
 * 	    body:
 * 	    {
 * 	        "id": "99"
 * 	    }
 * 	    (ideally ts should also be sent from client, but not done in this prototype)
 *
 * 	 2) GET /api/status?id=9,99,999
 *
 */
public class Server implements HttpHandler {
    private int PORT = 8080;
    private int BACKLOG = 0;
    private HttpServer server;

    private String GET_STATUS_URI = "/api/status";
    private String POST_HEARTBEAT_URI = "/api/heartbeat";

    private DBConnectionPool databasePool;
    private final String DB_URL = "jdbc:postgresql://localhost:5432/test";
    private final String DB_USER = "postgres";
    private final String DB_PASSWORD = "postgres";

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

        try {
            databasePool = new DBConnectionPool(DB_URL, DB_USER, DB_PASSWORD, 10);
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

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
                handleError(exchange);
                break;
        }
    }

    private void handleGetMethod(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();

        if (GET_STATUS_URI.equals(uri.getPath())) {
            String query = uri.getQuery();
            String[] args = query.split("=");

            if ("id".equals(args[0])) {
                String users = getOnlineUsersFromDb(args[1]);

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, users.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(users.getBytes());
                os.flush();
                os.close();
            }
            else
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }

    private void handlePostMethod(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();

        // Update user with current heartbeat time
        if (POST_HEARTBEAT_URI.equals(uri.getPath())) {
            InputStream reqBody = exchange.getRequestBody();
            String body = new String(reqBody.readAllBytes(), StandardCharsets.UTF_8);

            ObjectMapper objMapper = new ObjectMapper();
            JsonNode jsonNode = objMapper.readTree(body);

            int userId = jsonNode.get("id").asInt();
            Timestamp heartbeatTime = new Timestamp(System.currentTimeMillis());

            updateUserHeartbeartInDb(userId, heartbeatTime);

            String response = "heartbeat updated for user. id: " + userId + ", ts: " + heartbeatTime;
            exchange.sendResponseHeaders(200, response.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.flush();
            os.close();
        }
    }

    private void handleError(HttpExchange exchange) throws IOException {
        String response = "ERROR";
        exchange.sendResponseHeaders(405, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.flush();
        os.close();
    }

    private void updateUserHeartbeartInDb(int userID, Timestamp heartbeatTime) {
        Connection conn = null;

        try {
            conn = databasePool.getConnection();
            String query = "INSERT INTO user_heartbeats (id, last_hb) VALUES (?, ?)" +
                    " ON CONFLICT (id) DO UPDATE " +
                    " SET last_hb = ?";

            PreparedStatement preparedStatement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            preparedStatement.setInt(1, userID);
            preparedStatement.setTimestamp(2, heartbeatTime);
            preparedStatement.setTimestamp(3, heartbeatTime);

            preparedStatement.execute();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (conn != null)
                databasePool.returnConnection(conn);
        }
    }

    private String getOnlineUsersFromDb(String usersList) {
        Connection conn = null;
        String[] ids = usersList.split(",");

        try {
            conn = databasePool.getConnection();
            String query = "SELECT * FROM user_heartbeats WHERE id IN (" + usersList + ")";

            Statement statement = conn.createStatement();
            ResultSet resultSet =  statement.executeQuery(query);

            ObjectMapper objMapper = new ObjectMapper();
            ArrayNode arrayNode = objMapper.createArrayNode();

            while (resultSet.next()) {
                ObjectNode entry = objMapper.createObjectNode();

                entry.put("id", resultSet.getInt("id"));
                entry.put("lastActive", String.valueOf(resultSet.getTimestamp("last_hb")));

                arrayNode.add(entry);
            }

            return arrayNode.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (conn != null)
                databasePool.returnConnection(conn);
        }
        return "";
    }
}
