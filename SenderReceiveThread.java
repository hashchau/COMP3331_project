import java.io.*;
import java.util.*;
import java.net.*;


public class SenderReceiveThread implements Runnable {
    @Override
    public void run() {
        while (true) {

            try {
                // Lock the current thread
                Globals.syncLock.lock();

                int packetSize = Globals.headerSize + Globals.maxSegmentSize;
                byte[] receiveData = new byte[packetSize];
                DatagramPacket receivePacket = 
                new DatagramPacket(receiveData, receiveData.length);
                try {
                    Globals.senderSocket.receive(receivePacket);

                    // Get field values from received packet
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
                    
                    // If the entire file has already been sent successfully ACKed, return from the 
                    // thread
                    if (Globals.receiverAckNum >= (Globals.fileToSend.length() + 
                        Globals.initSeqNum)) {
                        return;
                    }

                    // Correct and expected ACK has been received, so update the sender's buffer
                    if (receivedPacket.getAckNum() == Globals.expectedAckNum) {
                        // Create a new buffer which only contains the packets that
                        // have not been acknowledged yet.
                        for (Packet currPacket: Globals.sendBuffer) {
                            ArrayList<Packet> tempBuffer = new ArrayList<>();
                            if ((currPacket.getSeqNum() + currPacket.getLength()) > Globals.expectedAckNum) {
                                tempBuffer.add(currPacket);
                            }
                            Globals.sendBuffer = tempBuffer;
                        }
                    // Duplicate ACK has been received
                    } else if (receivedPacket.getAckNum() == Globals.lastAckNum) {
                        Globals.numDupAcks += 1;
                        Globals.totalDupAcksReceived++;
                        // If the number of duplicate ACKs reaches 3, then use fast retransmit to
                        // retransmit the oldest unACKed packet.
                        if (Globals.numDupAcks == 3) {
                            // Retransmit oldest unACKed packet
                            Helper.retransmit(Globals.sendBuffer, Globals.lastAckNum);
                            // Reset the counter for the total number of received duplicate ACKs
                            Globals.numDupAcks = 0;
                        }
                    }
        
                    Globals.lastAckNum = receivedPacket.getAckNum();

                } catch (IOException e) {
                    // Do nothing
                }

            } catch (Exception e) {
                // Do nothing.
            } finally {
                // Unlock the current thread
                Globals.syncLock.unlock();
                // Let the thread sleep for the given time interval, which is defined in 
                // Globals.java
                try {
                    Thread.sleep(Globals.SENDER_RECEIVE_INTERVAL);
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }

        }
    }
}
