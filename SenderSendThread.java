import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.locks.*;

public class SenderSendThread implements Runnable {   

    @Override
    public void run() {
        while (true) {

            Globals.syncLock.lock();

            if (Globals.sumBytesRead >= Globals.fileToSend.length()) {
                Globals.syncLock.unlock();
                return;
            } else if (Globals.isAckReceived == true) {
                try {
                    DatagramPacket currPacket = Helper.createDataPacket();
                    Globals.senderSocket.send(currPacket);
                    Helper.logSend();
                    Globals.isAckReceived = false;      
                    Globals.expectedAckNum = Globals.senderSeqNum + Globals.bytesRead;                  
                    
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
