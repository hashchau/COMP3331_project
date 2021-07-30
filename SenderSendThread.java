import java.io.*;
import java.util.*;
import java.net.*;

public class SenderSendThread implements Runnable {
    private InetAddress receiverHostIP;
    private int receiverPort;
    private DatagramSocket senderSocket;
    private FileInputStream inFromFile;
    private byte[] fileData;
    private FileOutputStream logStream;
    private long start;
    private int senderNumBytes;
    private int bytesRead;
    private int senderSeqNum;
    private int senderAckNum;
    private int maxSegmentSize;
    private int maxWindowSize;


    public SenderSendThread(InetAddress receiverHostIP, int receiverPort, 
        DatagramSocket senderSocket, FileInputStream inFromFile, 
        byte[] fileData, FileOutputStream logStream, long start, 
        int senderNumBytes, int bytesRead, int senderSeqNum, int senderAckNum, 
        int maxSegmentSize, int maxWindowSize) {
        this.receiverHostIP = receiverHostIP;
        this.receiverPort = receiverPort;
        this.senderSocket = senderSocket;
        this.inFromFile = inFromFile;
        this.fileData = fileData;
        this.logStream = logStream;
        this.start = start;
        this.senderNumBytes = senderNumBytes;
        this.bytesRead = bytesRead;
        this.senderSeqNum = senderSeqNum;
        this.senderAckNum = senderAckNum;
        this.maxSegmentSize = maxSegmentSize;
        this.maxWindowSize = maxWindowSize;
    }
    

    @Override
    public void run() {
        try {
            while ((bytesRead = inFromFile.read(fileData)) != -1)  {

                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                DataOutputStream dataOut = new DataOutputStream(byteOut);
                dataOut.writeInt(senderSeqNum); // Sequence number
                dataOut.writeInt(senderAckNum); // ACK number
                dataOut.writeByte(0); // ACK flag
                dataOut.writeByte(0); // SYN flag
                dataOut.writeByte(0); // FIN flag
                dataOut.writeInt(maxSegmentSize); // MSS
                dataOut.writeInt(maxWindowSize); // MWS
                dataOut.write(fileData, 0, bytesRead);

                byte[] sendData = byteOut.toByteArray();
                dataOut.close();
                byteOut.close();

                DatagramPacket sendPacket = 
                new DatagramPacket(sendData, sendData.length, 
                    receiverHostIP, receiverPort);
                senderSocket.send(sendPacket);

                senderNumBytes = bytesRead;
                Logger.logData(logStream, "snd", 
                    Helper.elapsedTimeInMillis(start, System.nanoTime()), "D", 
                    senderSeqNum, senderNumBytes, senderAckNum);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
