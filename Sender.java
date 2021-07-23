import java.io.*;
import java.net.*;

public class Sender {
    // private static final int HEADERSIZE = 18;
	public static void main(String[] args) throws Exception {

        // Initialise seq and ack nums for logging.
        int senderSeqNum = 121;
        int senderNumBytes = 0;
        int senderAckNum = 0;
        int receiverNumBytes = 0;

		// Define socket parameters, address and Port No
        InetAddress receiverHostIP = InetAddress.getByName(args[0]);
		int receiverPort = Integer.parseInt(args[1]); 
		//change above port number if required
        String filename = args[2];
        int maxWindowSize = Integer.parseInt(args[3]);
        int maxSegmentSize = Integer.parseInt(args[4]);
        double timeout = Double.parseDouble(args[5]);
        double probabilityDrop = Double.parseDouble(args[6]);
        double seed = Double.parseDouble(args[7]);

        // Open Sender_log.txt for logging sender packets
        File senderLogFile = new File("Sender_log.txt");
        FileOutputStream logStream = new FileOutputStream(senderLogFile);
		
        // Record start time.
        long start = System.nanoTime();

		// Create socket, using any port, which connects to receiver
		DatagramSocket senderSocket = new DatagramSocket(0);

        // Send out SYN -------------------------------------------------------

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        dataOut.writeInt(senderSeqNum); // ISN
        dataOut.writeInt(senderAckNum); // ACK number
        dataOut.writeByte(0); // ACK flag
        dataOut.writeByte(1); // SYN flag
        dataOut.writeByte(0); // FIN flag
        dataOut.writeInt(maxSegmentSize); // MSS
        dataOut.writeInt(maxWindowSize); // MWS
        
        byte[] handshakeData = byteOut.toByteArray();
        
        DatagramPacket synPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                receiverHostIP, receiverPort);
        senderSocket.send(synPacket);

        Logger.logData(logStream, "snd", 
            Helper.elapsedTimeInMillis(start, System.nanoTime()), "S", 
            senderSeqNum, senderNumBytes, senderAckNum);

        // Receive SYN-ACK and send out ACK -----------------------------------

        byte[] receiveData = new byte[64];

        DatagramPacket receivePacket = 
            new DatagramPacket(receiveData, receiveData.length);
        senderSocket.receive(receivePacket);

        byte[] currBytes = receivePacket.getData();
        ByteArrayInputStream byteIn = new ByteArrayInputStream(currBytes);
        DataInputStream dataIn = new DataInputStream(byteIn);
        int receiverSeqNum = dataIn.readInt();
        int receiverAckNum = dataIn.readInt();
        int receiverAckFlag = dataIn.readByte();
        int receiverSynFlag = dataIn.readByte();
        int receiverFinFlag = dataIn.readByte();
        maxSegmentSize = dataIn.readInt();
        maxWindowSize = dataIn.readInt();

        // Update sender's seq number and ack number.
        senderSeqNum = receiverAckNum;
        senderAckNum = receiverSeqNum + 1;

        if (receiverSynFlag == 1 && receiverAckFlag == 1) {
            // System.err.println("Received SYN-ACK, so sending out ACK.");
            Logger.logData(logStream, "rcv", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "SA", 
                receiverSeqNum, receiverNumBytes, receiverAckNum);

            byteOut = new ByteArrayOutputStream();
            dataOut = new DataOutputStream(byteOut);
            dataOut.writeInt(senderSeqNum); // Sequence number
            dataOut.writeInt(senderAckNum); // ACK number
            dataOut.writeByte(1); // ACK flag
            dataOut.writeByte(0); // SYN flag
            dataOut.writeByte(0); // FIN flag
            dataOut.writeInt(maxSegmentSize); // MSS
            dataOut.writeInt(maxWindowSize); // MWS

            handshakeData = byteOut.toByteArray();
            DatagramPacket ackPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                receiverHostIP, receiverPort);
            senderSocket.send(ackPacket);

            Logger.logData(logStream, "snd", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                senderSeqNum, senderNumBytes, senderAckNum);
        }

        // Send out file ------------------------------------------------------

        // // get input from file
        // File fileToSend = new File(filename);
        // FileInputStream inFromFile = new FileInputStream(fileToSend);
        // int i = 1;
        // //prepare for sending
        // byte[] sendData = new byte[64];

        // while ((inFromFile.read(sendData)) != -1) {

        //     // write to receiver, need to create DatagramPacket with receiver 
        //     // address and port No
        //     DatagramPacket sendPacket = 
        //         new DatagramPacket(sendData, sendData.length, 
        //         receiverHostIP, receiverPort);
        //     //actual send call
        //     senderSocket.send(sendPacket);
        //     String currLine = new String(sendData);
        //     System.err.print("Sent string " + i + ": " + currLine);
        //     i++;
        // }

        // File transferred so send out FIN -----------------------------------

        // System.err.println("Data transferred, so sending out FIN.");

        byteOut = new ByteArrayOutputStream();
        dataOut = new DataOutputStream(byteOut);
        dataOut.writeInt(senderSeqNum); // Sequence number
        dataOut.writeInt(senderAckNum); // ACK number
        dataOut.writeByte(0); // ACK flag
        dataOut.writeByte(0); // SYN flag
        dataOut.writeByte(1); // FIN flag
        dataOut.writeInt(maxSegmentSize); // MSS
        dataOut.writeInt(maxWindowSize); // MWS

        handshakeData = byteOut.toByteArray();
        DatagramPacket finPacket = 
        new DatagramPacket(handshakeData, handshakeData.length, 
            receiverHostIP, receiverPort);
        senderSocket.send(finPacket);
        Logger.logData(logStream, "snd", 
            Helper.elapsedTimeInMillis(start, System.nanoTime()), "F", 
            senderSeqNum, senderNumBytes, senderAckNum);


        // Receive server's FIN-ACK and send out ACK ------------------------------
        
        receiveData = new byte[64];

        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        senderSocket.receive(receivePacket);

        currBytes = receivePacket.getData();
        byteIn = new ByteArrayInputStream(currBytes);
        dataIn = new DataInputStream(byteIn);
        receiverSeqNum = dataIn.readInt();
        receiverAckNum = dataIn.readInt();
        receiverAckFlag = dataIn.readByte();
        receiverSynFlag = dataIn.readByte();
        receiverFinFlag = dataIn.readByte(); 

        // Update sender's seq number and ack number.
        senderSeqNum = receiverAckNum;
        senderAckNum = receiverSeqNum + 1;

        if (receiverFinFlag == 1 && receiverAckFlag == 1) {
            // System.err.println("Received server's FIN-ACK so sending out ACK.");
            Logger.logData(logStream, "rcv", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "FA", 
                receiverSeqNum, receiverNumBytes, receiverAckNum);

            byteOut = new ByteArrayOutputStream();
            dataOut = new DataOutputStream(byteOut);
            dataOut.writeInt(senderSeqNum); // Sequence number
            dataOut.writeInt(senderAckNum); // ACK number
            dataOut.writeByte(1); // ACK flag
            dataOut.writeByte(0); // SYN flag
            dataOut.writeByte(0); // FIN flag
            dataOut.writeInt(maxSegmentSize); // MSS
            dataOut.writeInt(maxWindowSize); // MWS
    
            handshakeData = byteOut.toByteArray();
            DatagramPacket ackPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                receiverHostIP, receiverPort);
            senderSocket.send(ackPacket);
            Logger.logData(logStream, "snd", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                senderSeqNum, senderNumBytes, senderAckNum);
        }

        // Close the socket ---------------------------------------------------

        senderSocket.close();
        logStream.close();
        // inFromFile.close();
		
	} // end of main

} // end of class Sender

