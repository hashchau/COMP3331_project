import java.io.*;
import java.util.*;
import java.net.*;

public class Helper {

    // Used for debugging
    // Prints the values that will be contained inside a packet's header
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

    // Find the difference in time between two given times
    public static double elapsedTimeInMillis(long start, long current) {
        long elapsedNanoSecs = current - start;
        double elapsedMilliSecs = elapsedNanoSecs / 1000000.0;
        return elapsedMilliSecs;
    }

    // Use the given header field values to generate a byte array to store in a packet.
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

    // Read at most MSS bytes of data from the input file and create a UDP Datagram Packet using
    // this data as well as global header values
    public static DatagramPacket createDataPacket() throws IOException {
        Globals.bytesRead = Globals.inFromFile.read(Globals.fileData);

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        dataOut.writeInt(Globals.senderSeqNum); // Sequence number
        dataOut.writeInt(Globals.senderAckNum); // ACK number
        dataOut.writeByte(0); // ACK flag
        dataOut.writeByte(0); // SYN flag
        dataOut.writeByte(0); // FIN flag
        dataOut.writeInt(Globals.maxSegmentSize); // MSS
        dataOut.writeInt(Globals.maxWindowSize); // MWS
        dataOut.write(Globals.fileData, 0, Globals.bytesRead);

        byte[] sendData = byteOut.toByteArray();

        Globals.sumBytesRead += Globals.bytesRead;

        DatagramPacket sendPacket = 
        new DatagramPacket(sendData, sendData.length, 
            Globals.receiverHostIP, Globals.receiverPort);

        return sendPacket;
        
    }

    // Add a line to Sender_log.txt to log a dropped packet
    public static void logSend(int seqNum, int numBytes, int ackNum) {
        Globals.senderNumBytes = Globals.bytesRead;
                    
        Logger.logData(Globals.logStream, "snd", 
            Helper.elapsedTimeInMillis(Globals.start, System.nanoTime()), "D", 
            seqNum, numBytes, ackNum);
    }

    // Add a line to Sender_log.txt to log a dropped packet
    public static void logDrop(int seqNum, int numBytes, int ackNum) {
        Globals.senderNumBytes = Globals.bytesRead;
                    
        Logger.logData(Globals.logStream, "drop", 
            Helper.elapsedTimeInMillis(Globals.start, System.nanoTime()), "D", 
            seqNum, numBytes, ackNum);
    }

    // PL module implementation
    // Decides whether to drop the next sent packet or not
    public static boolean isPacketDropped() {
        if (Globals.randomGen.nextFloat() > Globals.probabilityDrop) {
            return false;
        } else {
            return true;
        }
    } 

    // Retransmit the packet with the given seqNum as long as it's in the sender's buffer
    public static void retransmit(ArrayList<Packet> sendBuffer, int seqNum) throws IOException {
        for (Packet currPacket : sendBuffer) {
            if (currPacket.getSeqNum() == seqNum) {
                // Globals.timerStart = System.nanoTime();
                currPacket.setTimeSent(System.nanoTime());
                DatagramPacket sendPacket = currPacket.createDatagramPacket();
                // System.err.println("Resending dropped packet.");
                Globals.senderSocket.send(sendPacket);
                Helper.logSend(
                    currPacket.getSeqNum(),
                    currPacket.getDataLength(),
                    currPacket.getAckNum()
                );

                Globals.totalRetransmittedSegments++;
            }
        }
    }

}
