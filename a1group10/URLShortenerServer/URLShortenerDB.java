import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class URLShortenerDB {
    private BlockingQueue<Connection> connPool;

    public URLShortenerDB(String url, int coonPoolSize) {
        connPool = new ArrayBlockingQueue<>(coonPoolSize);
        for (int i = 0; i < coonPoolSize; i++) {
            try {
                Connection conn = DriverManager.getConnection(url);
                String sql = """
                        pragma synchronous = normal;
                        pragma journal_mode = WAL;
                        """;
                Statement stmt = conn.createStatement();
                stmt.executeUpdate(sql);
                connPool.offer(conn);
            } catch (SQLException se) {
                System.err.println("Database error: " + se.getMessage());
            }
        }
    }

    public URLShortenerDB(int numConn) {
        this("jdbc:sqlite:" + "/virtual/" + System.getProperty("user.name") + "/urlshortener.db", numConn);
    }

    public boolean save(String shortURL, String longURL) {
        Connection conn = null;
        try {
            conn = connPool.take();
            String insertSQL = "INSERT INTO urls(shorturl,longurl) VALUES(?,?) ON CONFLICT(shorturl) DO UPDATE SET longurl=?;";
            PreparedStatement ps = conn.prepareStatement(insertSQL);
            ps.setString(1, shortURL);
            ps.setString(2, longURL);
            ps.setString(3, longURL);
            ps.execute();
            return true;
        } catch (InterruptedException | SQLException e) {
            System.err.println("Save error: " + e.getMessage());
            return false;
        } finally {
            if (conn != null)
                try {
                    connPool.put(conn);
                } catch (InterruptedException e) {
                    System.err.println("Save error: " + e.getMessage());
                }
        }
    }

    public String find(String shortURL) {
        Connection conn = null;
        try {
            conn = connPool.take();
            String findSQL = "SELECT longurl FROM urls WHERE shorturl=?;";
            PreparedStatement ps = conn.prepareStatement(findSQL);
            ps.setString(1, shortURL);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("longurl");
            }
            return null;
        } catch (InterruptedException | SQLException e) {
            System.err.println("Find error: " + e.getMessage());
            return null;
        } finally {
            if (conn != null)
                try {
                    connPool.put(conn);
                } catch (InterruptedException e) {
                    System.err.println("Save error: " + e.getMessage());
                }
        }
    }
}
