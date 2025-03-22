package org.example.database;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnectionPool {
    private int poolSize = 10;
    private String url;
    private String user;
    private String password;

    private MyBlockingQueue<Connection> pool;

    public DBConnectionPool(String url, String user, String password, int capacity) throws SQLException, ClassNotFoundException {
        this.url = url;
        this.user = user;
        this.password = password;
        this.poolSize = capacity;

        pool = new MyBlockingQueue<Connection>(this.poolSize);
        Class.forName("org.postgresql.Driver");

        for (int i = 0; i < this.poolSize; ++i)
            pool.add(DBConnectionFactory.createConnection(this.url, this.user, this.password));
    }

    public Connection getConnection() {
        return pool.get();
    }

    public void returnConnection(Connection connection) {
        pool.add(connection);
    }

    public void destroyPool() throws SQLException {
        while (!pool.isEmpty()) {
            Connection conn = pool.get();
            conn.close();
        }
    }
}