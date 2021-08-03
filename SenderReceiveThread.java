import java.io.*;
import java.util.*;
import java.net.*;


public class SenderReceiveThread implements Runnable {
    @Override
    public void run() {
        while (true) {

            Globals.syncLock.lock();

            // System.err.println("This is entered.");

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
                    Globals.maxWindowSize, null, System.nanoTime());

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
                    // System.err.println("ACK received with number: " + receivedPacket.getAckNum());
                    for (Packet currPacket: Globals.sendBuffer) {
                        ArrayList<Packet> tempBuffer = new ArrayList<>();
                        // if ((currPacket.getSeqNum() + currPacket.getLength()) > Globals.expectedAckNum) {
                        if (currPacket.getSeqNum() >= Globals.expectedAckNum) {
                        // if (currPacket.getSeqNum() > Globals.expectedAckNum) {
                            tempBuffer.add(currPacket);
                        }
                        Globals.sendBuffer = tempBuffer;
                    }
                } 
            
                else if (receivedPacket.getAckNum() == Globals.lastAckNum) {
                    // System.err.println("Received a duplicate ACK!");
                    Globals.numDupAcks += 1;
                    Globals.totalDupAcksReceived++;
                    if (Globals.numDupAcks == 3) {
                        // Retransmit oldest unACKed packet
                        // System.err.println("Retransmitting oldest unACKed packet.");
                        Helper.retransmit(Globals.sendBuffer, Globals.lastAckNum);
                        Globals.numDupAcks = 0;
                    }
                }
    
                Globals.lastAckNum = receivedPacket.getAckNum();

            } catch (IOException e) {
                // do nothing
            }

            Globals.syncLock.unlock();

            try {
                Thread.sleep(Globals.SENDER_RECEIVE_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
