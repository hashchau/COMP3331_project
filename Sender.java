import java.io.*;
import java.net.*;
import java.util.*;

public class Sender {
    private static final int headerSize = 19;
	public static void main(String[] args) throws Exception {

        // Initialise seq and ack nums for logging.

        Globals.senderSeqNum = Globals.initSeqNum;
        Globals.senderAckNum = 0;
        Globals.senderNumBytes = 0;
        Globals.receiverNumBytes = 0;

		// Define socket parameters, address and Port No
        Globals.receiverHostIP = InetAddress.getByName(args[0]);
		Globals.receiverPort = Integer.parseInt(args[1]); 
        Globals.filename = args[2];
        Globals.maxWindowSize = Integer.parseInt(args[3]);
        Globals.maxSegmentSize = Integer.parseInt(args[4]);
        Globals.timeout = Double.parseDouble(args[5]);
        Globals.probabilityDrop = Float.parseFloat(args[6]);
        Globals.seed = Long.parseLong(args[7]);

        // Calculate packet size from given MSS and designed header size.
        int packetSize = headerSize + Globals.maxSegmentSize;

        // Open Sender_log.txt for logging sender packets
        File senderLogFile = new File("Sender_log.txt");
        Globals.logStream = new FileOutputStream(senderLogFile);
		
        // Record Globals.start time.
        Globals.start = System.nanoTime();

		// Create socket, using any port, which connects to receiver
		Globals.senderSocket = new DatagramSocket(0);

        try {
            Globals.senderSocket.setSoTimeout(Globals.SOCKET_TIMEOUT);
        } catch (SocketException e) {
            // Do nothing.
        }
        

        // Send out SYN -------------------------------------------------------

        byte[] handshakeData = Helper.makePacketBytes(Globals.senderSeqNum, 
            Globals.senderAckNum, 0, 1, 0, Globals.maxSegmentSize, Globals.maxWindowSize);
        
        DatagramPacket synPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                Globals.receiverHostIP, Globals.receiverPort);
        Globals.senderSocket.send(synPacket);

        Logger.logData(Globals.logStream, "snd", 
            Helper.elapsedTimeInMillis(Globals.start, System.nanoTime()), "S", 
            Globals.senderSeqNum, Globals.senderNumBytes, Globals.senderAckNum);

        // Receive SYN-ACK and send out ACK -----------------------------------

        byte[] receiveData = new byte[packetSize];

        DatagramPacket receivePacket = 
            new DatagramPacket(receiveData, receiveData.length);
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

        // Update sender's seq number and ack number.
        Globals.senderSeqNum = Globals.receiverAckNum;
        Globals.senderAckNum = Globals.receiverSeqNum + 1;

        if (receiverSynFlag == 1 && receiverAckFlag == 1) {
            // System.err.println("Received SYN-ACK, so sending out ACK.");
            Logger.logData(Globals.logStream, "rcv", 
                Helper.elapsedTimeInMillis(Globals.start, System.nanoTime()), "SA", 
                Globals.receiverSeqNum, Globals.receiverNumBytes, Globals.receiverAckNum);

            handshakeData = Helper.makePacketBytes(Globals.senderSeqNum, 
            Globals.senderAckNum, 1, 0, 0, Globals.maxSegmentSize, Globals.maxWindowSize);

            DatagramPacket ackPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                Globals.receiverHostIP, Globals.receiverPort);
            Globals.senderSocket.send(ackPacket);

            Logger.logData(Globals.logStream, "snd", 
                Helper.elapsedTimeInMillis(Globals.start, System.nanoTime()), "A", 
                Globals.senderSeqNum, Globals.senderNumBytes, Globals.senderAckNum);
        }

        // SEND OUT FILE ------------------------------------------------------

        // Generate random number for dropping packets
        Globals.randomGen = new Random(Globals.seed);

        // Create packet buffer for sending data
        Globals.sendBuffer = new ArrayList<>();

        // get input from file
        Globals.fileToSend = new File(Globals.filename);
        Globals.inFromFile = new FileInputStream(Globals.fileToSend);

        //prepare for sending
        Globals.fileData = new byte[Globals.maxSegmentSize];
        Globals.senderNumBytes = Globals.maxSegmentSize;
        // int Globals.bytesRead;

        SenderSendThread sst = new SenderSendThread();
        Thread sendingThread = new Thread(sst); 
        SenderReceiveThread srt = new SenderReceiveThread();
        Thread receivingThread = new Thread(srt);

        sendingThread.start();
        receivingThread.start();

        sendingThread.join();
        receivingThread.join();


        // File transferred so send out FIN -----------------------------------

        Globals.senderNumBytes = 0;

        Globals.senderSeqNum = Globals.receiverAckNum;

        handshakeData = Helper.makePacketBytes(Globals.senderSeqNum, 
            Globals.senderAckNum, 0, 0, 1, Globals.maxSegmentSize, Globals.maxWindowSize);

        DatagramPacket finPacket = 
        new DatagramPacket(handshakeData, handshakeData.length, 
            Globals.receiverHostIP, Globals.receiverPort);
        Globals.senderSocket.send(finPacket);
        Logger.logData(Globals.logStream, "snd", 
            Helper.elapsedTimeInMillis(Globals.start, System.nanoTime()), "F", 
                Globals.senderSeqNum, Globals.senderNumBytes, Globals.senderAckNum);


        // Receive server's FIN-ACK and send out ACK ------------------------------


        receiveData = new byte[packetSize];

        receivePacket = new DatagramPacket(receiveData, receiveData.length);

        Globals.senderSocket.setSoTimeout(0);

        Globals.senderSocket.receive(receivePacket);

        // System.err.println("FIN-ACK packet is this long: " + receivePacket.getLength());

        currBytes = receivePacket.getData();
        byteIn = new ByteArrayInputStream(currBytes);
        dataIn = new DataInputStream(byteIn);
        Globals.receiverSeqNum = dataIn.readInt();
        Globals.receiverAckNum = dataIn.readInt();
        receiverAckFlag = dataIn.readByte();
        receiverSynFlag = dataIn.readByte();
        receiverFinFlag = dataIn.readByte(); 

        // Update sender's seq number and ack number.
        Globals.senderSeqNum = Globals.receiverAckNum;
        Globals.senderAckNum = Globals.receiverSeqNum + 1;

        if (receiverFinFlag == 1 && receiverAckFlag == 1) {
            // System.err.println("Received server's FIN-ACK so sending out ACK.");
            Logger.logData(Globals.logStream, "rcv", 
                Helper.elapsedTimeInMillis(Globals.start, System.nanoTime()), "FA", 
                Globals.receiverSeqNum, Globals.receiverNumBytes, Globals.receiverAckNum);

            handshakeData = Helper.makePacketBytes(Globals.senderSeqNum, 
                Globals.senderAckNum, 1, 0, 0, Globals.maxSegmentSize, Globals.maxWindowSize);

            DatagramPacket ackPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                Globals.receiverHostIP, Globals.receiverPort);
            Globals.senderSocket.send(ackPacket);
            Logger.logData(Globals.logStream, "snd", 
                Helper.elapsedTimeInMillis(Globals.start, System.nanoTime()), "A", 
                Globals.senderSeqNum, Globals.senderNumBytes, Globals.senderAckNum);
        }

        // Log summary stats
        Logger.logSenderStats(Globals.logStream);

        // Close the socket ---------------------------------------------------

        Globals.senderSocket.close();
        Globals.logStream.close();
        Globals.inFromFile.close();

	} // end of main

} // end of class Sender

