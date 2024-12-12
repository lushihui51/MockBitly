import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import html.GenerateHeader;
import html.ReadFileData;

public class URLShortener {
    static final File WEB_ROOT = new File(System.getProperty("user.home") + "/a1group10/html");
    static final String DEFAULT_FILE = "index.html";
    static final String NOT_FOUND = "404.html";
    static final String RECEIVED = "received.html";
    static final String TEMPORARY_REDIRECT = "307.html";

    static URLShortenerDB database = null;
    static final int PORT = 3000;
    static final boolean verbose = false;

    public static void main(String[] args) {
        boolean listening = true;
        database = new URLShortenerDB(500);
        try (ServerSocket server = new ServerSocket(PORT);) {
            InetAddress inetAddress = InetAddress.getLocalHost();
            String hostName = inetAddress.getHostName();
            System.out.println(hostName + "'s URLShortener is listening on port: " + PORT);

            while (listening) {
                Socket clientSocket = server.accept();
                URLShortenerWorker w = new URLShortenerWorker(clientSocket);
                Thread t = new Thread(w);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Server connection error: \n" + e.getMessage());
            System.exit(1);
        }
    }

    static class URLShortenerWorker implements Runnable {
        Socket clienSocket = null;

        public URLShortenerWorker(Socket clientSocket) {
            this.clienSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(this.clienSocket.getInputStream()));
                    PrintWriter clientOut = new PrintWriter(this.clienSocket.getOutputStream());
                    BufferedOutputStream clientDataOut = new BufferedOutputStream(this.clienSocket.getOutputStream())) {

                String method;
                String shortURL;
                String longURL;
                while ((method = clientIn.readLine()) != null) {
                    shortURL = clientIn.readLine();
                    longURL = clientIn.readLine();
                    System.out.println("this node's database has received method: " + method);
                    System.out.println("this node's database has received shortURL: " + shortURL);
                    System.out.println("this node's database has received longURL: " + longURL);

                    File file;
                    int fileLength;
                    byte[] fileData;
                    String httpHeader;
                    if ("PUT".equals(method)) {
                        database.save(shortURL, longURL);
                        file = new File(WEB_ROOT, RECEIVED);
                        fileLength = (int) file.length();
                        fileData = ReadFileData.readFileData(file, fileLength);
                        httpHeader = GenerateHeader.generateHeader("200 OK", "N/A", "URLShortener", new Date(),
                                "text/html",
                                fileLength);

                    } else if ("GET".equals(method)) {
                        String longStored = database.find(shortURL);
                        if (longStored != null) {
                            file = new File(WEB_ROOT, TEMPORARY_REDIRECT);
                            fileLength = (int) file.length();
                            fileData = ReadFileData.readFileData(file, fileLength);
                            httpHeader = GenerateHeader.generateHeader("307 Temporary Redirect", longStored,
                                    "URLShortener", new Date(), "text/html", fileLength);
                        } else {
                            file = new File(WEB_ROOT, NOT_FOUND);
                            fileLength = (int) file.length();
                            fileData = ReadFileData.readFileData(file, fileLength);

                            httpHeader = GenerateHeader.generateHeader("404 Not Found", "N/A", "URLShortener",
                                    new Date(),
                                    "text/html", fileLength);
                        }
                    } else {
                        throw new Exception("Unknown method.");
                    }
                    clientOut.print(httpHeader);
                    clientOut.flush();
                    clientDataOut.write(fileData, 0, fileLength);
                    clientDataOut.flush();
                }
            } catch (Exception e) {
                System.err.println("Server error: " + e.getMessage());
            }
        }
    }

}
