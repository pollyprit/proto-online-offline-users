package org.example.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CleaningJob extends Thread {
    private long runIntervalSec;
    private Connection connection;
    private boolean stop = false;
    private long ttlSec;

    public CleaningJob(String url, String user, String password, long runIntervalSec, long ttlSec) throws ClassNotFoundException, SQLException {
        this.runIntervalSec = runIntervalSec;
        this.ttlSec = ttlSec;
        Class.forName("org.postgresql.Driver");
        connection = DBConnectionFactory.createConnection(url, user, password);

        System.out.println("cleanup job started. run internal(sec) " + runIntervalSec + ", ttl(sec) " + ttlSec);
    }

    @Override
    public void run() {
        // DELETE FROM user_heartbeats WHERE (current_timestamp - last_hb) > interval '? seconds'
        String query = "DELETE FROM user_heartbeats" +
                " WHERE (current_timestamp - last_hb ) > interval '" + ttlSec + " seconds'";
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

        while (!stop) {
            try {
                Thread.sleep(runIntervalSec * 1000);
            } catch (InterruptedException e) {
                // ignore
            }

            try {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                int rowsDel = preparedStatement.executeUpdate();

                System.out.println("[" + dateFormat.format(new Date()) +
                            "] cleanup job: rows deleted " + rowsDel);
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

    }

    public void stopCleaningJob() {
        this.stop = true;
    }
}
