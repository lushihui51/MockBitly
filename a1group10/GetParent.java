import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

public class GetParent {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java GetParent <server1> <server2> ...");
            return;
        }
        TreeMap<String, String> hosts = new TreeMap<>();
        for (int i = 1; i < args.length; i++) {
            String hashed = hash(args[i]);
            hosts.put(hashed, args[i]);
        }
        String hashed = hash(args[0]);
        Map.Entry<String, String> entry = hosts.lowerEntry(hashed);
        entry = entry == null ? hosts.lastEntry() : entry;
        System.out.println(entry.getValue());
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

}
