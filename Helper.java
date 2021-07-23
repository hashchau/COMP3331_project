import java.io.*;
import java.net.*;

public class Helper {
    public static void printHeader(int seqNum, int ackNum, int ackFlag, 
        int synFlag, int finFlag, int maxSegmentSize, int maxWindowSize) {
        System.err.print(
            "Sequence number: " + seqNum + "\n"
            + "ACK number: " + ackNum + "\n" 
            + "ACK flag: " + ackFlag + "\n"
            + "SYN flag: " + synFlag + "\n" 
            + "FIN flag: " + finFlag + "\n" 
            + "MSS: " + maxSegmentSize + "\n" 
            + "MWS: " + maxWindowSize + "\n" 
        );
    }

    public static double elapsedTimeInMillis(long start, long current) {
        long elapsedNanoSecs = current - start;
        double elapsedMilliSecs = elapsedNanoSecs / 1000000.0;
        return elapsedMilliSecs;
    }

    public static byte[] makePacketBytes(int seqNum, int ackNum,
        int ackFlag, int synFlag, int finFlag, int maxSegmentSize,
        int maxWindowSize) throws IOException {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(byteOut);
            dataOut.writeInt(seqNum); // ISN
            dataOut.writeInt(ackNum); // ACK number
            dataOut.writeByte(ackFlag); // ACK flag
            dataOut.writeByte(synFlag); // SYN flag
            dataOut.writeByte(finFlag); // FIN flag
            dataOut.writeInt(maxSegmentSize); // MSS
            dataOut.writeInt(maxWindowSize); // MWS
            return byteOut.toByteArray();
    }

}
