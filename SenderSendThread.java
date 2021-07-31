import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.locks.*;

public class SenderSendThread implements Runnable {   

    @Override
    public void run() {
        while (true) {

            Globals.syncLock.lock();

            if (Globals.isConnected == true) {
                if (Globals.sumBytesRead >= Globals.fileToSend.length()) {
                    Globals.syncLock.unlock();
                    return;
                } else if (Globals.isAckReceived == true) {
                    try {
                        Globals.bytesRead = Globals.inFromFile.read(Globals.fileData);
                        Globals.sumBytesRead += Globals.bytesRead;

                        // Send out data.
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
                        dataOut.close();
                        byteOut.close();      

                        DatagramPacket sendPacket = 
                        new DatagramPacket(sendData, sendData.length, 
                            Globals.receiverHostIP, Globals.receiverPort);
                        Globals.senderSocket.send(sendPacket);
            
                        Globals.senderNumBytes = Globals.bytesRead;
                        Logger.logData(Globals.logStream, "snd", 
                            Helper.elapsedTimeInMillis(Globals.start, System.nanoTime()), "D", 
                            Globals.senderSeqNum, Globals.senderNumBytes, Globals.senderAckNum);

                        Globals.isAckReceived = false;      
                        Globals.expectedAckNum = Globals.senderSeqNum + Globals.bytesRead;                  
                        
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            Globals.syncLock.unlock();

            try {
                Thread.sleep(Globals.UPDATE_INTERVAL);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            
        }
    }
}
