import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.locks.*;

public class SenderReceiveThread implements Runnable {
    @Override
    public void run() {
        while (true) {

            Globals.syncLock.lock();

            System.err.println("Start of SRT.");

            int packetSize = Globals.headerSize + Globals.maxSegmentSize;
            byte[] receiveData = new byte[packetSize];
            DatagramPacket receivePacket = 
            new DatagramPacket(receiveData, receiveData.length);
            try {
                // Globals.syncLock.lock();
                // System.err.println("Before receive.");
                Globals.senderSocket.receive(receivePacket);
                // System.err.println("After receive.");
                byte[] currBytes = receivePacket.getData();
                ByteArrayInputStream byteIn = new ByteArrayInputStream(currBytes);
                DataInputStream dataIn = new DataInputStream(byteIn);
                Globals.receiverSeqNum = dataIn.readInt();
                Globals.receiverAckNum = dataIn.readInt();
                int receiverAckFlag = dataIn.readByte();
                int receiverSynFlag = dataIn.readByte();
                int receiverFinFlag = dataIn.readByte();
                Globals.maxSegmentSize = dataIn.readInt();
                Globals.maxWindowSize = dataIn.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }


            Logger.logData(Globals.logStream, "rcv", 
            Helper.elapsedTimeInMillis(Globals.start, System.nanoTime()), "A", 
                Globals.receiverSeqNum, Globals.receiverNumBytes, Globals.receiverAckNum);
            Globals.senderSeqNum = Globals.receiverAckNum;

            Globals.isAckReceived = true;

            if (Globals.receiverAckNum >= Globals.fileToSend.length()) {
                Globals.syncLock.unlock();
                return;
            }

            System.err.println("End of SRT.");

            Globals.syncLock.unlock();

            try {
                Thread.sleep(Globals.UPDATE_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            

        }
    }
}
