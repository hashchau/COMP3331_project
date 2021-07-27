import java.io.*;
import java.net.*;

public class Sender {
    private static final int headerSize = 19;
	public static void main(String[] args) throws Exception {

        // Initialise seq and ack nums for logging.
        int senderSeqNum = 121;
        int senderAckNum = 0;
        int senderNumBytes = 0;
        int receiverNumBytes = 0;

		// Define socket parameters, address and Port No
        InetAddress receiverHostIP = InetAddress.getByName(args[0]);
		int receiverPort = Integer.parseInt(args[1]); 
        String filename = args[2];
        int maxWindowSize = Integer.parseInt(args[3]);
        int maxSegmentSize = Integer.parseInt(args[4]);
        double timeout = Double.parseDouble(args[5]);
        double probabilityDrop = Double.parseDouble(args[6]);
        double seed = Double.parseDouble(args[7]);

        // Calculate packet size from given MSS and designed header size.
        int packetSize = headerSize + maxSegmentSize;

        // Open Sender_log.txt for logging sender packets
        File senderLogFile = new File("Sender_log.txt");
        FileOutputStream logStream = new FileOutputStream(senderLogFile);
		
        // Record start time.
        long start = System.nanoTime();

		// Create socket, using any port, which connects to receiver
		DatagramSocket senderSocket = new DatagramSocket(0);

        // Send out SYN -------------------------------------------------------

        byte[] handshakeData = Helper.makePacketBytes(senderSeqNum, 
            senderAckNum, 0, 1, 0, maxSegmentSize, maxWindowSize);
        
        DatagramPacket synPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                receiverHostIP, receiverPort);
        senderSocket.send(synPacket);

        Logger.logData(logStream, "snd", 
            Helper.elapsedTimeInMillis(start, System.nanoTime()), "S", 
            senderSeqNum, senderNumBytes, senderAckNum);

        // Receive SYN-ACK and send out ACK -----------------------------------

        byte[] receiveData = new byte[packetSize];

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

            handshakeData = Helper.makePacketBytes(senderSeqNum, 
            senderAckNum, 1, 0, 0, maxSegmentSize, maxWindowSize);

            DatagramPacket ackPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                receiverHostIP, receiverPort);
            senderSocket.send(ackPacket);

            Logger.logData(logStream, "snd", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                senderSeqNum, senderNumBytes, senderAckNum);
        }

        // SEND OUT FILE ------------------------------------------------------

        // get input from file
        File fileToSend = new File(filename);
        FileInputStream inFromFile = new FileInputStream(fileToSend);

        //prepare for sending
        byte[] fileData = new byte[maxSegmentSize];
        senderNumBytes = maxSegmentSize;
        int bytesRead;

        while ((bytesRead = inFromFile.read(fileData)) != -1) {
            System.err.println("bytesRead == " + bytesRead);

            // Send out data.
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(byteOut);
            dataOut.writeInt(senderSeqNum); // Sequence number
            dataOut.writeInt(senderAckNum); // ACK number
            dataOut.writeByte(0); // ACK flag
            dataOut.writeByte(0); // SYN flag
            dataOut.writeByte(0); // FIN flag
            dataOut.writeInt(maxSegmentSize); // MSS
            dataOut.writeInt(maxWindowSize); // MWS
            dataOut.write(fileData, 0, bytesRead);

            System.err.println("fileData == " + fileData.length);

            byte[] sendData = byteOut.toByteArray();
            dataOut.close();
            byteOut.close();
            
            DatagramPacket sendPacket = 
                new DatagramPacket(sendData, sendData.length, 
                receiverHostIP, receiverPort);
            senderSocket.send(sendPacket);

            System.out.println("sendPacket == " + sendPacket.getLength());

            senderNumBytes = bytesRead;
            Logger.logData(logStream, "snd", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "D", 
                senderSeqNum, senderNumBytes, senderAckNum);

            // Receive ack.

            receivePacket = 
                new DatagramPacket(receiveData, receiveData.length);
            senderSocket.receive(receivePacket);

            currBytes = receivePacket.getData();
            byteIn = new ByteArrayInputStream(currBytes);
            dataIn = new DataInputStream(byteIn);
            receiverSeqNum = dataIn.readInt();
            receiverAckNum = dataIn.readInt();
            receiverAckFlag = dataIn.readByte();
            receiverSynFlag = dataIn.readByte();
            receiverFinFlag = dataIn.readByte();
            maxSegmentSize = dataIn.readInt();
            maxWindowSize = dataIn.readInt();

            Logger.logData(logStream, "rcv", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                receiverSeqNum, receiverNumBytes, receiverAckNum);

            senderSeqNum = receiverAckNum;

            // // Increment counters
            // if (receiverAckNum == (senderSeqNum + bytesRead)) {
            //     Logger.logData(logStream, "rcv", 
            //         Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
            //         receiverSeqNum, receiverNumBytes, receiverAckNum);
            //     senderSeqNum += bytesRead;
            // }
            
        }

        // File transferred so send out FIN -----------------------------------

        senderNumBytes = 0;

        handshakeData = Helper.makePacketBytes(senderSeqNum, 
            senderAckNum, 0, 0, 1, maxSegmentSize, maxWindowSize);

        DatagramPacket finPacket = 
        new DatagramPacket(handshakeData, handshakeData.length, 
            receiverHostIP, receiverPort);
        senderSocket.send(finPacket);
        Logger.logData(logStream, "snd", 
            Helper.elapsedTimeInMillis(start, System.nanoTime()), "F", 
                senderSeqNum, senderNumBytes, senderAckNum);


        // Receive server's FIN-ACK and send out ACK ------------------------------
        
        receiveData = new byte[packetSize];

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

            handshakeData = Helper.makePacketBytes(senderSeqNum, 
                senderAckNum, 1, 0, 0, maxSegmentSize, maxWindowSize);

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
        inFromFile.close();
		
	} // end of main

} // end of class Sender

