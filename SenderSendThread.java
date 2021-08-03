import java.io.*;
import java.util.*;
import java.net.*;

public class SenderSendThread implements Runnable {   

    public static void checkTimeout(ArrayList<Packet> sendBuffer) throws IOException {
        if (sendBuffer.size() > 0) {
            double elapsedTime = Helper.elapsedTimeInMillis(sendBuffer.get(0).getTimeSent(),
                System.nanoTime());
            if (elapsedTime > Globals.timeout) {
                // Retransmit the packet with sequence number equal to the 
                // last ACK number from the receiver because that's the packet
                // that the receiver wants.
                // System.err.println("Retransmitting due to timeout");
                Helper.retransmit(sendBuffer, Globals.lastAckNum);
            }
        }
    }

    @Override
    public void run() {
        while (true) {

            Globals.syncLock.lock();

            try {
                checkTimeout(Globals.sendBuffer);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            if (Globals.sumBytesRead >= Globals.fileToSend.length()) {
                // if (Globals.sendBuffer.size() > 0) {
                //     continue;
                // }
                Globals.syncLock.unlock();
                return;
            } 
            
            // System.err.println("expectedAckNum == " + Globals.expectedAckNum);
            // System.err.println("lastAckNum == " + Globals.lastAckNum);

            Globals.lastByteSent = Globals.expectedAckNum;
            Globals.lastByteAcked = Globals.lastAckNum;
            
            // if (Globals.isAckReceived == true) {
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

                    // System.err.println("Sending:");
                    // for (Packet bufferPacket : Globals.sendBuffer) {
                    //     System.err.println("\t" + bufferPacket.getSeqNum());
                    // }

                    // System.err.println("Sending packet with sequence number: " + currPacket.getSeqNum());
                    currPacket.setTimeSent(System.nanoTime());

                    if (Helper.isPacketDropped() == true) {
                        // Don't send anything; just log the drop.
                        Helper.logDrop(
                            currPacket.getSeqNum(),
                            currPacket.getDataLength(),
                            currPacket.getAckNum()
                        );
                        // System.err.println("Packet dropped!");
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
                    e.printStackTrace();
                }
            }

            Globals.syncLock.unlock();

            try {
                Thread.sleep(Globals.SENDER_SEND_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
   
        }
    }
}
