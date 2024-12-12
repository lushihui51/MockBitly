import java.io.*;
import java.net.*;

public class testServer {
    public static void main(String[] args) throws IOException {
        int portNumber = 3000;
        boolean listening = true;
        InetAddress inetAddress = InetAddress.getLocalHost();
        String hostName = inetAddress.getHostName();
        System.out.println(hostName + "'s testServer listening on port 3000");

        try (ServerSocket server = new ServerSocket(portNumber)) {
            while (listening) {
                Socket clientSocket = server.accept();
                TestWorker t = new TestWorker(clientSocket);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + portNumber);
            System.exit(1);
        }
    }

    static class TestWorker extends Thread {

        Socket clienSocket = null;

        public TestWorker(Socket clientSocket) {
            this.clienSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(this.clienSocket.getInputStream()));
                    PrintWriter clientOut = new PrintWriter(this.clienSocket.getOutputStream())) {

                // You should receive: <method>\n<shortURL>\n<longURL>\n
                // long url could be null, depedning on the method

                String method = clientIn.readLine();
                String shortURL = clientIn.readLine();
                String longURL = clientIn.readLine();

                System.out.println("End server received: ");
                System.out.println("Method: " + method);
                System.out.println("shortURL: " + shortURL);
                System.out.println("longURL: " + longURL);

                // Do some work
                Thread.sleep(1000);
                // A Simple HTTP response:
                // Could just send a custom response, and let the proxyServer turn it into HTTP
                // Simple HTTP response
                String httpResponse = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: 9\r\n" +
                        "\r\n" +
                        "Received!";
                // Send
                clientOut.print(httpResponse);
                clientOut.flush();
            } catch (IOException e) {
                System.err.println("TestWorker failed.");
                System.exit(1);
            } catch (InterruptedException ie) {
                System.err.println("TestWorker interrupted while doing work.");
            }
        }
    }
}