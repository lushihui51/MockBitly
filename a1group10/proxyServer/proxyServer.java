import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import html.GenerateHeader;
import html.ReadFileData;

// Our proxy server to handle reverse proxying
public class proxyServer {
  static final File WEB_ROOT = new File(System.getProperty("user.home") + "/a1group10/html");
  static final String METHOD_NOT_ALLOWED = "405.html";
  static final String BAD_REQUEST = "400.html";
  static final String NOT_FOUND = "404.html";
  static final String WELCOME = "index.html";
  static final String LOG_PATH = System.getProperty("user.home") + "/a1group10/logs/writes/";
  static final Map<String, FileHandler> FILE_HANDLERS = new HashMap<>();

  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.err.println("Usage: java ProxyServer <server1> <server2> ...");
      return;
    }

    System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tFT%1$tT%n%5$s");

    // Default port for the end servers (URL shortener services) to listen on is
    // port 3000, on their systems
    int remotePort = 3000;
    // Default port for this proxy server to listen on is port 8080, on the local
    // system
    int localPort = 8080;

    // A binary sort tree with entries:
    // - key: hashed values of the machines
    // - value: machines
    // Sorted by the keys (hashes)
    TreeMap<String, String> hosts = new TreeMap<>();
    InetAddress inetAddress = InetAddress.getLocalHost();
    String hostName = inetAddress.getHostName();
    System.out.println(hostName + " starting proxy for: ");
    for (int i = 0; i < args.length; i++) {
      String hashed = hash(args[i]);
      hosts.put(hashed, args[i]);
      FileHandler fileHandler = new FileHandler(LOG_PATH + args[i] + ".log", true);
      fileHandler.setFormatter(new SimpleFormatter());
      FILE_HANDLERS.put(args[i], fileHandler);
      System.out.println(args[i] + ":" + remotePort);
    }
    System.out.println("On port: " + localPort);

    // Start listening on local port for incoming requests from clients
    try (ServerSocket proxyServer = new ServerSocket(localPort);) {
      while (true) {
        Socket newClientSocket = proxyServer.accept();
        // Multi-threading to let a worker to handle an incoming request
        Thread t = new ProxyWorker(hosts, remotePort, newClientSocket, FILE_HANDLERS);
        t.start();
      }
    } catch (Exception e) {
      System.err.println(e);
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

  static class ProxyWorker extends Thread {
    TreeMap<String, String> hosts;
    int remotePort;
    Socket clientSocket;
    int serverNum;
    Map<String, FileHandler> fileHandlers;
    Logger logger;

    // serverNum is the number of servers assigned to any individual shortURL. It is
    // the minimum between the number of hosts and the serverNumSpecified parameter.
    // - Defualt serverNumSpecified is 2
    public ProxyWorker(TreeMap<String, String> hosts, int remotePort, Socket clientSocket,
        Map<String, FileHandler> fileHandlers, int serverNumSpecified) {
      this.hosts = hosts;
      this.remotePort = remotePort;
      this.clientSocket = clientSocket;
      this.fileHandlers = fileHandlers;
      this.logger = Logger.getLogger("logger-" + UUID.randomUUID());
      this.logger.setLevel(java.util.logging.Level.INFO);
      // this.logger.setUseParentHandlers(false);
      this.serverNum = Math.min(serverNumSpecified, hosts.size());
    }

    public ProxyWorker(TreeMap<String, String> hosts, int remotePort, Socket clientSocket,
        Map<String, FileHandler> fileHandler) {
      this(hosts, remotePort, clientSocket, fileHandler, 2);
    }

    @Override
    public void run() {
      try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
          PrintWriter clientOut = new PrintWriter(this.clientSocket.getOutputStream(), true);
          BufferedOutputStream clientDataOut = new BufferedOutputStream(this.clientSocket.getOutputStream())) {

        // Extracting the HTTP Request
        String inputLine = clientIn.readLine();
        if (inputLine == null) {
          return;
        }
        System.out.println("inpuLine: " + inputLine);
        Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
        Matcher mput = pput.matcher(inputLine);
        Pattern pget = Pattern.compile("^GET\\s+/(\\S+)\\s+(\\S+)$");
        Matcher mget = pget.matcher(inputLine);
        String method = null;
        String shortURL = null;
        String longURL = null;

        if (mput.matches()) {
          method = "PUT";
          shortURL = mput.group(1);
          longURL = mput.group(2);
        } else if (mget.matches()) {
          method = "GET";
          shortURL = mget.group(1);
        } else {
          File file;
          int fileLength;
          byte[] fileData;
          String httpHeader;
          String code;

          if ("GET / HTTP/1.1".equals(inputLine)) {
            file = new File(WEB_ROOT, WELCOME);
            code = "200 OK";
          } else if (inputLine.startsWith("PUT") || inputLine.startsWith("GET")) {
            file = new File(WEB_ROOT, BAD_REQUEST);
            code = "400 Bad Request";
          } else {
            file = new File(WEB_ROOT, METHOD_NOT_ALLOWED);
            code = "405 Method Not Allowed";
          }
          fileLength = (int) file.length();
          fileData = ReadFileData.readFileData(file, fileLength);
          httpHeader = GenerateHeader.generateHeader(code, "N/A", "URLShortener", new Date(), "text/html", fileLength);
          clientOut.print(httpHeader);
          clientOut.flush();
          clientDataOut.write(fileData, 0, fileLength);
          clientDataOut.flush();
          return;
        }
        System.out.println("method: " + method);
        System.out.println("shortURL: " + shortURL);
        System.out.println("longURL: " + longURL);

        // Find servers, by hashing the shortURL and find the server with a hash just
        // higher than the shortURL's hash
        // For more than 1 server, find the next server with the lowest hash
        // If a hash is the highest, to find the next hash we loop back to the lowest
        String hashed = hash(shortURL);
        String[] validServers = new String[this.serverNum];
        for (int i = 0; i < this.serverNum; i++) {
          Map.Entry<String, String> entry = hosts.higherEntry(hashed);
          entry = entry == null ? hosts.firstEntry() : entry;
          validServers[i] = entry.getValue();
          hashed = entry.getKey();
          logger.addHandler(fileHandlers.get(validServers[i]));
          System.out.println(i + "th valid server is: " + validServers[i]);
        }

        if ("PUT".equals(method)) {
          logger.info(method + "\n" + shortURL + "\n" + longURL + "\n");
          int sent = 0;
          for (String server : validServers) {
            try (Socket serverSocket = new Socket(server, this.remotePort);
                BufferedReader serverIn = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                PrintWriter serverOut = new PrintWriter(serverSocket.getOutputStream())) {
              serverOut.println(method);
              serverOut.println(shortURL);
              serverOut.println(longURL);
              serverOut.flush();

              String responseLine;
              while ((responseLine = serverIn.readLine()) != null) {
                clientOut.println(responseLine);
              }
              clientOut.flush();
              sent += 1;

            } catch (IOException e) {
              System.out.println("System: " + server + " is down");
            } finally {
              logger.removeHandler(fileHandlers.get(server));
            }
          }
          System.out.println("PUT request sent to: " + sent + "/" + validServers.length + "valid servers.");
        } else {
          boolean got = false;
          int visited = 0;
          while (!got) {
            String server = validServers[visited];
            try (Socket serverSocket = new Socket(server, this.remotePort);
                BufferedReader serverIn = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                PrintWriter serverOut = new PrintWriter(serverSocket.getOutputStream())) {
              serverOut.println(method);
              serverOut.println(shortURL);
              serverOut.println(longURL);
              serverOut.flush();

              String responseLine = serverIn.readLine();
              if ("HTTP/1.1 307 Temporary Redirect".equals(responseLine) || visited == validServers.length - 1) {
                got = true;
                clientOut.println(responseLine);
                while ((responseLine = serverIn.readLine()) != null) {
                  clientOut.println(responseLine);
                }
                clientOut.flush();
              }
            } catch (IOException e) {
              System.out.println("System: " + server + " is down");
              if (visited == validServers.length - 1) {
                got = true;
                File file = new File(WEB_ROOT, NOT_FOUND);
                int fileLength = (int) file.length();
                byte[] fileData = ReadFileData.readFileData(file, fileLength);
                String code = "404 Not Found";
                String httpHeader = GenerateHeader.generateHeader(code, "N/A", "URLShortener", new Date(), "text/html",
                    fileLength);
                clientOut.print(httpHeader);
                clientOut.flush();
                clientDataOut.write(fileData, 0, fileLength);
                clientDataOut.flush();
              }
            } finally {
              visited += 1;
            }
          }
          System.out.println("GET request sent to: " + visited + "/" + validServers.length + "valid servers.");
        }

      } catch (IOException e) {
        System.err.println("Error handling client connection: " + e.getMessage());
      }

    }
  }
}