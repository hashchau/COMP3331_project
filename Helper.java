
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

    // public static byte[] intToByteArray(int i) {
    //     ByteBuffer currBuffer = ByteBuffer.allocate(4);
    //     currBuffer.putInt(i);
    //     return currBuffer.array();
    // }

    // public static int byteArrayToInt(byte[] intBytes) {
    //     ByteBuffer currBuffer = ByteBuffer.allocate(8);
    //     currBuffer.put(intBytes);
    //     return currBuffer.getInt();
    // }
}
