import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

public class MonitoringSystem {
    static final int PORT = 3001;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java ProxyServer <server1> <server2> ...");
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            InetAddress inetAddress = InetAddress.getLocalHost();
            String hostName = inetAddress.getHostName();
            System.out.println(hostName + "'s monitor is listening on port: " + PORT);

            Monitor monitor = new Monitor(args, hostName);
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(monitor, 5, 5, TimeUnit.SECONDS);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {

                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Monitoring system error: " + e.getMessage());
            System.exit(1);
        }
    }

    // A hash function to hash a string into another string, using MD5
    public static String hash(String input) {
        StringBuilder hexString = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
        } catch (Exception e) {
            System.err.println("hash failed.");
        }
        return hexString.toString();
    }

    static class Monitor implements Runnable {
        String thisHost;
        TreeMap<String, String> hosts = new TreeMap<>();
        Stack<String> currentlyMonitoring = new Stack<>();
        Map<String, Boolean[]> status = new HashMap<>();
        static final int proxyServerPort = 8080;
        static final int URLShortenerPort = 3000;
        static final int monitoringSystemPort = 3001;
        static final String STATUS_LOG = System.getProperty("user.home") + "/a1group10/logs/status/";

        public Monitor(String[] args, String thisHost) {
            for (int i = 0; i < args.length; i++) {
                String hashed = hash(args[i]);
                hosts.put(hashed, args[i]);
            }
            String hashed = hash(thisHost);
            Map.Entry<String, String> entry = hosts.higherEntry(hashed);
            entry = entry == null ? hosts.firstEntry() : entry;
            currentlyMonitoring.push(entry.getValue());
            this.thisHost = thisHost;
        }

        boolean isPortOpen(String host, int port) {
            try (Socket socket = new Socket(host, port)) {
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        void report() {
            for (String host : currentlyMonitoring) {
                try (PrintWriter writer = new PrintWriter(STATUS_LOG + host + ".log");) {
                    // Build your message
                    Boolean[] stats = status.get(host);

                    String proxyStat = stats[0] ? "UP" : "DOWN";
                    String shortenerStat = stats[1] ? "UP" : "DOWN";
                    String monitorStat = stats[2] ? "UP" : "DOWN";
                    String msg = "TIME: " + new Date() + "\n" +
                            host + "'s status monitored by: " + thisHost + "\n" +
                            "Proxy Server: " + proxyStat + "\n" +
                            "URLShortener Server: " + shortenerStat + "\n" +
                            "Monitoring System: " + monitorStat + "\n";

                    // Write the message to the file (this replaces all previous content)
                    writer.print(msg);
                    writer.flush();

                } catch (Exception e) {
                    System.err.println("Monitor report error for host: " + host);
                }
            }
        }

        @Override
        public void run() {
            String toAdd = null;
            String monitoringUntil = null;

            for (String node : currentlyMonitoring) {
                Boolean[] oldStatus = status.remove(node);
                Boolean[] nodeStatus = new Boolean[3];
                nodeStatus[0] = isPortOpen(node, proxyServerPort);
                nodeStatus[1] = isPortOpen(node, URLShortenerPort);
                nodeStatus[2] = isPortOpen(node, monitoringSystemPort);
                status.put(node, nodeStatus);

                if (nodeStatus[1] && oldStatus != null && !oldStatus[1]) {
                    System.out.println("Starting recovery for: " + node);
                    Thread t = new Thread(new RecoveryWorker(node));
                    t.start();
                }

                if (nodeStatus[2]) {
                    monitoringUntil = node;
                    break;
                } else {
                    String next;
                    Map.Entry<String, String> entry = hosts.higherEntry(hash(node));
                    entry = entry == null ? hosts.firstEntry() : entry;
                    next = entry.getValue();
                    if (!currentlyMonitoring.contains(next) && !next.equals(thisHost)) {
                        toAdd = next;
                    }
                }
            }
            report();
            if (monitoringUntil != null && toAdd != null) {
                System.err.println("Monitor running error: both adding and deleting from currentlyMonitoring");
            }
            if (monitoringUntil != null) {
                while (!currentlyMonitoring.peek().equals(monitoringUntil)) {
                    currentlyMonitoring.pop();
                }
            }
            if (toAdd != null) {
                currentlyMonitoring.push(toAdd);
            }
        }

        static class RecoveryWorker implements Runnable {
            static final String WRITES_LOG = System.getProperty("user.home") + "/a1group10/logs/writes/";
            String node;
            int shortenerPort;

            public RecoveryWorker(String node) {
                this.node = node;
                this.shortenerPort = 3000;
            }

            @Override
            public void run() {
                System.out.println("running as a RecoveryWorker");
                System.out.println("requesting writes from log: " + WRITES_LOG + node + ".log");
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
                    System.err.println("Recovery error: " + e.getMessage());
                }
            }
        }
    }
}
