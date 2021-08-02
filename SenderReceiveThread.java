import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.locks.*;

public class SenderReceiveThread implements Runnable {
    @Override
    public void run() {
        while (true) {

            Globals.syncLock.lock();

            int packetSize = Globals.headerSize + Globals.maxSegmentSize;
            byte[] receiveData = new byte[packetSize];
            DatagramPacket receivePacket = 
            new DatagramPacket(receiveData, receiveData.length);
            try {
                Globals.senderSocket.receive(receivePacket);

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

                Packet receivedPacket = new Packet(Globals.receiverSeqNum, 
                    Globals.receiverAckNum, receiverAckFlag, receiverSynFlag, 
                    receiverFinFlag, Globals.maxSegmentSize, 
                    Globals.maxWindowSize, null);

                Logger.logData(Globals.logStream, "rcv", 
                Helper.elapsedTimeInMillis(Globals.start, System.nanoTime()), "A", 
                    receivedPacket.getSeqNum(), 0, receivedPacket.getAckNum());
                
                if (Globals.receiverAckNum >= (Globals.fileToSend.length() + 
                    Globals.initSeqNum)) {
                    Globals.syncLock.unlock();
                    return;
                }

                if (receivedPacket.getAckNum() == Globals.expectedAckNum) {
                    // Create a new buffer which only contains the packets that
                    // have not been acknowledged yet.
                    for (Packet currPacket: Globals.sendBuffer) {
                        ArrayList<Packet> tempBuffer = new ArrayList<>();
                        // if ((currPacket.getSeqNum() + currPacket.getLength()) > Globals.expectedAckNum) {
                        // if (currPacket.getSeqNum() >= Globals.expectedAckNum) {
                        if (currPacket.getSeqNum() > Globals.expectedAckNum) {
                            tempBuffer.add(currPacket);
                        }
                        Globals.sendBuffer = tempBuffer;
                    }
                } 
            
                else if (receivedPacket.getAckNum() == Globals.lastAckNum) {
                    System.err.println("Received a duplicate ACK!");
                    Globals.numDupAcks += 1;
                    if (Globals.numDupAcks == 3) {
                        // Retransmit oldest unACKed packet
                        System.err.println("Retransmitting oldest unACKed packet.");
                        Helper.retransmit(Globals.sendBuffer, Globals.lastAckNum);
                        Globals.numDupAcks = 0;
                    }
                }
    
                Globals.isAckReceived = true;
                Globals.lastAckNum = receivedPacket.getAckNum();

            } catch (IOException e) {
                // do nothing
            }

            Globals.syncLock.unlock();

            try {
                Thread.sleep(Globals.UPDATE_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
