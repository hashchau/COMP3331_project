import java.io.FileOutputStream;
import java.io.IOException;

public class Logger {
    public static void logData(FileOutputStream logStream, String packetStatus, 
        double elapsedTime, String packetType, int seqNum, int numBytes, 
        int ackNum) {
        String logString = String.format(
            "%-4s  %10.3f  %-3S  %-5d  %-5d  %-5d\n", packetStatus, elapsedTime, 
            packetType, seqNum, numBytes, ackNum
        );
        byte[] logBytes = logString.getBytes();
        try {
            logStream.write(logBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Debugging print statement.
        // System.err.print(logString);

        // If necessary, use StringBuffer class to create string.
        // Learn how to format tab-separated string.
    }

    public static void logSenderStats(FileOutputStream logStream) {
        StringBuffer statString = new StringBuffer("");
        statString.append("Amount of (original) Data Transferred (in bytes): " + 
            Globals.totalOriginalBytesTransferred + "\n");
        statString.append("Number of Data Segments Sent (excluding retransmissions): " + 
            Globals.totalSegmentsSent + "\n");
        statString.append("Number of (all) Packets Dropped (by the PL module): " + 
            Globals.totalPacketsDropped + "\n");
        statString.append("Number of Retransmitted Segments: " + 
            Globals.totalRetransmittedSegments + "\n");
        statString.append("Number of Duplicate Acknowledgements received: " + 
            Globals.totalDupAcksReceived + "\n");
        
        String stats = statString.toString();
        try {
            logStream.write(stats.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Debugging print statement.
        // System.err.print(stats);
    }

    public static void logReceiverStats(FileOutputStream logStream) {
        StringBuffer statString = new StringBuffer("");
        statString.append("Amount of (original) Data Received (in bytes): " + 
            Globals.totalOriginalBytesReceived + "\n");
        statString.append("Number of (original) Data Segments Received: " + 
            Globals.totalSegmentsReceived + "\n");
        statString.append("Number of duplicate segments received (if any): " + 
            Globals.totalDupSegmentsReceived + "\n");
        
        String stats = statString.toString();
        try {
            logStream.write(stats.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Debugging print statement.
        // System.err.print(stats);
    }
}
