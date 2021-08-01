import java.io.FileOutputStream;
import java.io.IOException;

public class Logger {
    public static void logData(FileOutputStream logStream, String packetStatus, 
        double elapsedTime, String packetType, int seqNum, int numBytes, 
        int ackNum) {
        String logString = String.format(
            "%-5s %-10.3f %-5S %-5d %-5d %-5d\n", packetStatus, elapsedTime, 
            packetType, seqNum, numBytes, ackNum
        );
        byte[] logBytes = logString.getBytes();
        try {
            logStream.write(logBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Debugging print statement.
        System.err.print(logString);

        // If necessary, use StringBuffer class to create string.
        // Learn how to format tab-separated string.
    }
}
