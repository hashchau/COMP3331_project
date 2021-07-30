import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.net.*;

public class Globals {
    // Constants
    public static final long UPDATE_INTERVAL = 100;
    public static final int SOCKET_TIMEOUT = 1000;

    // Arg-related
    public static InetAddress receiverHostIP;
    public static int receiverPort;
    public static String filename;
    public static int maxSegmentSize;
    public static int maxWindowSize;
    public static double timeout;
    public static float probabilityDrop;
    public static long seed;
    // General
    public static int headerSize = 19;
    public static int packetSize;
    public static DatagramSocket senderSocket;
    public static File fileToSend;
    public static FileInputStream inFromFile;
    public static byte[] fileData;
    public static FileOutputStream logStream;
    public static long start;
    public static ReentrantLock syncLock = new ReentrantLock();
    // Packet-related

    public static int senderNumBytes;
    public static int receiverNumBytes;
    public static int bytesRead;
    public static int senderSeqNum;
    public static int senderAckNum;
    public static int receiverSeqNum;
    public static int receiverAckNum;

    public static int expectedAckNum;
    // Extra
    public static int sumBytesRead = 0;
    public static boolean isConnected = false;
    public static boolean isAckReceived = true;
    // public static boolean fileSent = false;
}
