import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.locks.*;

public class SenderSendThread implements Runnable {   



    public static void checkTimeout() throws IOException {
        if (Globals.sendBuffer.size() > 0) {
            double elapsedTime = Helper.elapsedTimeInMillis(Globals.timerStart, 
                System.nanoTime());
            if (elapsedTime > Globals.timeout) {
                // Retransmit the packet with sequence number equal to the 
                // last ACK number from the receiver because that's the packet
                // that the receiver wants.
                Helper.retransmit();
            }
        }
    }

    @Override
    public void run() {
        while (true) {

            Globals.syncLock.lock();

            try {
                checkTimeout();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            if (Globals.sumBytesRead >= Globals.fileToSend.length()) {
                Globals.syncLock.unlock();
                return;
            } 
            
            System.err.println("expectedAckNum == " + Globals.expectedAckNum);
            System.err.println("lastAckNum == " + Globals.lastAckNum);

            Globals.lastByteSent = Globals.expectedAckNum;
            Globals.lastByteAcked = Globals.lastAckNum;
            
            if (Globals.isAckReceived == true) {
            // if ((Globals.lastByteSent - Globals.lastByteAcked) <= Globals.maxWindowSize) {
                try {
                    // Create a packet with filled header fields but no data.
                    Packet currPacket = new Packet(Globals.senderSeqNum, 
                        Globals.senderAckNum, 0, 0, 0, Globals.maxSegmentSize, 
                        Globals.maxWindowSize, null);
                    // Read from input file and add data to packet.
                    currPacket.getData();
                    // Add the packet to the buffer.
                    Globals.sendBuffer.add(currPacket);

                    Globals.expectedAckNum = Globals.senderSeqNum + currPacket.getDataLength();
                    Globals.senderSeqNum += currPacket.getDataLength();   

                    System.err.println("Packets currently in send buffer:");
                    for (Packet bufferPacket : Globals.sendBuffer) {
                        System.err.println("\t" + bufferPacket.getSeqNum());
                    }

                    Globals.timerStart = System.nanoTime();  

                    if (Helper.isPacketDropped() == true) {
                        // Don't send anything; just log the drop.
                        Helper.logDrop(
                            currPacket.getSeqNum(),
                            currPacket.getDataLength(),
                            currPacket.getAckNum()
                        );
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
                    Globals.isAckReceived = false;


                } catch (IOException e) {
                    e.printStackTrace();
                }
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
