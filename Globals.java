import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.net.*;

public class Globals {
    // Constants
    public static final long SENDER_SEND_INTERVAL = 100;
    public static final long SENDER_RECEIVE_INTERVAL = 100;
    public static final int SOCKET_TIMEOUT = 1000;
    public static final int RECEIVE_BUFFER_SIZE = 250000;
    public static final int HEADER_SIZE = 19;

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
    public static Condition syncCondition = syncLock.newCondition();

    // Packet-related

    public static int senderNumBytes;
    public static int receiverNumBytes;
    public static int bytesRead;
    public static int initSeqNum = 0;
    public static int senderSeqNum;
    public static int senderAckNum;
    public static int receiverSeqNum;
    public static int receiverAckNum;

    // Extra
    public static int sumBytesRead = 0;
    public static boolean isConnected = false;
    public static boolean isAckReceived = true;
    public static Random randomGen;
    public static long timerStart;
    public static ArrayList<Packet> sendBuffer;
    public static int expectedAckNum = 0;
    public static int lastAckNum = 0;
    // public static int lastSeqNum;
    public static int numDupAcks = 0;
    public static int lastByteSent;
    public static int lastByteAcked;

    // Summary stats for sender
    public static int totalOriginalBytesTransferred = 0;
    public static int totalSegmentsSent = 0;
    public static int totalPacketsDropped = 0;
    public static int totalRetransmittedSegments = 0;
    public static int totalDupAcksReceived = 0;

    // Summary stats for receiver
    public static int totalOriginalBytesReceived = 0;
    public static int totalSegmentsReceived = 0;
    public static int totalDupSegmentsReceived = 0;
}
