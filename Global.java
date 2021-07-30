import java.io.*;
import java.util.*;
import java.net.*;

public class Global {
    // General
    public static InetAddress receiverHostIP;
    public static int receiverPort;
    public static DatagramSocket senderSocket;
    public static FileInputStream inFromFile;
    public static byte[] fileData;
    public static FileOutputStream logStream;
    public static long start;
    // Packet-related
    public static int senderNumBytes;
    public static int receiverNumBytes;
    public static int bytesRead;
    public static int senderSeqNum;
    public static int senderAckNum;
    public static int receiverSeqNum;
    public static int receiverAckNum;
    public static int receiverAckFlag;
    public static int receiverSynFlag;
    public static int receiverFinFlag;
    public static int maxSegmentSize;
    public static int maxWindowSize;
    // Extra
    public static int sumBytesRead;
    public static int isConnected;
}
