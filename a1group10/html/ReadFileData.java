package html;

import java.io.File;
import java.io.FileInputStream;

public class ReadFileData {
    public static byte[] readFileData(File file, int fileLength) {
        byte[] fileData = new byte[fileLength];
        try (FileInputStream fileIn = new FileInputStream(file);) {
            fileIn.read(fileData);

        } catch (Exception e) {
            System.err.println("Read file error: " + e.getMessage());
            System.exit(1);
        }
        return fileData;
    }
}
