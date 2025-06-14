import java.io.*;
import java.util.*;
import java.net.*;

public class SenderSendThread implements Runnable {   

    // Checks whether a timeout has occurred; if so, retransmit the oldest unACKed packet
    public static void checkTimeout(ArrayList<Packet> sendBuffer) throws IOException {
        if (sendBuffer.size() > 0) {
            double elapsedTime = Helper.elapsedTimeInMillis(sendBuffer.get(0).getTimeSent(),
                System.nanoTime());
            // If a timeout has been detected,
            // retransmit the packet with sequence number equal to the 
            // last ACK number from the receiver because that's the packet
            // that the receiver wants.
            if (elapsedTime > Globals.timeout) {
                
                Helper.retransmit(sendBuffer, Globals.lastAckNum);
            }
        }
    }

    @Override
    public void run() {
        while (true) {

            try {
                // Lock the current thread
                Globals.syncLock.lock();

                // Check if a timeout has occurred
                try {
                    checkTimeout(Globals.sendBuffer);
                } catch (IOException e1) {
                    // Do nothing
                }

                if (Globals.sumBytesRead >= Globals.fileToSend.length()) {
                    return;
                } 

                Globals.lastByteSent = Globals.expectedAckNum;
                Globals.lastByteAcked = Globals.lastAckNum;
                
                if ((Globals.lastByteSent - Globals.lastByteAcked) <= Globals.maxWindowSize) {
                    try {
                        // Create a packet with filled header fields but no data.
                        Packet currPacket = new Packet(Globals.senderSeqNum, 
                            Globals.senderAckNum, 0, 0, 0, Globals.maxSegmentSize, 
                            Globals.maxWindowSize, null, System.nanoTime());
                        // Read from input file and add data to packet.
                        currPacket.getData();
                        // Add the packet to the buffer.
                        Globals.sendBuffer.add(currPacket);

                        Globals.expectedAckNum = Globals.senderSeqNum + currPacket.getDataLength();
                        Globals.senderSeqNum += currPacket.getDataLength();   

                        currPacket.setTimeSent(System.nanoTime());

                        // Pass the packet through the PL module, which decides whether it should
                        // be dropped or not
                        if (Helper.isPacketDropped() == true) {
                            // Don't send anything; just log the drop.
                            Helper.logDrop(
                                currPacket.getSeqNum(),
                                currPacket.getDataLength(),
                                currPacket.getAckNum()
                            );
                            Globals.totalPacketsDropped++;
                        } else {
                            DatagramPacket sendPacket = currPacket.createDatagramPacket();
                            Globals.senderSocket.send(sendPacket);
                            // Log the send.
                            Helper.logSend(
                                currPacket.getSeqNum(),
                                currPacket.getDataLength(),
                                currPacket.getAckNum()
                            );  
                        }
                        // Globals.isAckReceived = false;


                    } catch (IOException e) {
                        // Do nothing
                    }
                }

            } catch (Exception e) {
                // Do nothing
            } finally {
                // Unlock the current thread
                Globals.syncLock.unlock();
                // Let the thread sleep for the given time interval, which is defined in 
                // Globals.java
                try {
                    Thread.sleep(Globals.SENDER_SEND_INTERVAL);
                } catch (InterruptedException e) {
                    // Do nothing
                }

            }
   
        }
    }
}
