package html;

import java.util.Date;

public class GenerateHeader {

    public static String generateHeader(String code, String location, String server, Date date, String contentType,
            int contentLength) {

        return "HTTP/1.1 " + code + "\r\n" +
                "Location: " + location + "\r\n" +
                "Server: " + server + "\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Content-type: " + contentType + "\r\n" +
                "Content-length: " + contentLength + "\r\n" +
                "\r\n";
    }
}
