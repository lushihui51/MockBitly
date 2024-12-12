import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Birth {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java Birth <server1> <server2> ...");
            return;
        }

        for (String node : args) {
            Thread t = new Thread(new Birther(node));
            t.start();
        }
    }

    static class Birther implements Runnable {
        static final String WRITES_LOG = System.getProperty("user.home") + "/a1group10/logs/writes/";
        String node;
        int shortenerPort;

        public Birther(String node) {
            this.node = node;
            this.shortenerPort = 3000;
        }

        @Override
        public void run() {
            try (FileInputStream fstream = new FileInputStream(WRITES_LOG + node + ".log");
                    BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                    Socket serverSocket = new Socket(node, shortenerPort);
                    PrintWriter serverOut = new PrintWriter(serverSocket.getOutputStream())) {
                Pattern dateTimeP = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$");
                String strline;
                while ((strline = br.readLine()) != null) {
                    Matcher dateTimeM = dateTimeP.matcher(strline);
                    if (dateTimeM.matches()) {
                        serverOut.println(br.readLine());
                        serverOut.println(br.readLine());
                        serverOut.println(br.readLine());
                        serverOut.flush();
                    }
                }
            } catch (Exception e) {
                System.err.println("Brith error: " + e.getMessage());
            }
        }

    }
}
