import java.io.*;
import java.net.*;

public class Receiver {

    private static final int headerSize = 19;
    private static final int maxUDPDatagramSize = 65515;

	public static void main(String[] args) throws Exception {

        // Initialise seq and ack nums for logging.
        int receiverSeqNum = 154;
        int receiverAckNum = 0;
        int receiverNumBytes = 0;
        int senderNumBytes = 0;

		int receiverPort = Integer.parseInt(args[0]);
        String outputFilename = args[1];

        // Open Sender_log.txt for logging sender packets
        File senderLogFile = new File("Receiver_log.txt");
        FileOutputStream logStream = new FileOutputStream(senderLogFile);
		
        // Record start time.
        long start = System.nanoTime();

		DatagramSocket receiverSocket = new DatagramSocket(receiverPort);
        System.out.println("Receiver is ready:");
        
        // Receive SYN and send out SYN-ACK -----------------------------------

        byte[] receiveData = new byte[maxUDPDatagramSize];

        DatagramPacket receivePacket = 
            new DatagramPacket(receiveData, receiveData.length);
        receiverSocket.receive(receivePacket);
       
        // Get info of the sender with whom we are communicating
        InetAddress senderHostIP = receivePacket.getAddress();
        int senderPort = receivePacket.getPort();

        byte[] currBytes = receivePacket.getData();
        ByteArrayInputStream byteIn = new ByteArrayInputStream(currBytes);
        DataInputStream dataIn = new DataInputStream(byteIn);
        int senderSeqNum = dataIn.readInt();
        int senderAckNum = dataIn.readInt();
        int senderAckFlag = dataIn.readByte();
        int senderSynFlag = dataIn.readByte();
        int senderFinFlag = dataIn.readByte();
        int maxSegmentSize = dataIn.readInt();
        int maxWindowSize = dataIn.readInt();

        // Calculate packet size from given MSS and designed header size.
        int packetSize = headerSize + maxSegmentSize;
        
        // Make data buffer for received packets the correct size. 
        receiveData = new byte[packetSize];

        receiverAckNum = senderSeqNum + 1;

        if (senderSynFlag == 1) {
            // System.err.println("Received SYN, so sending out SYN-ACK.");
            Logger.logData(logStream, "rcv", 
            Helper.elapsedTimeInMillis(start, System.nanoTime()), "S", 
            senderSeqNum, senderNumBytes, senderAckNum);

            byte[] handshakeData = Helper.makePacketBytes(receiverSeqNum, 
                receiverAckNum, 1, 1, 0, maxSegmentSize, maxWindowSize);

            DatagramPacket ackPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                senderHostIP, senderPort);
            receiverSocket.send(ackPacket);

            Logger.logData(logStream, "snd", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "SA", 
                receiverSeqNum, receiverNumBytes, receiverAckNum);
        }

        // Receive ACK --------------------------------------------------------

        receiverSocket.receive(receivePacket);

        currBytes = receivePacket.getData();
        byteIn = new ByteArrayInputStream(currBytes);
        dataIn = new DataInputStream(byteIn);
        senderSeqNum = dataIn.readInt();
        senderAckNum = dataIn.readInt();
        senderAckFlag = dataIn.readByte();
        senderSynFlag = dataIn.readByte();
        senderFinFlag = dataIn.readByte();
        maxSegmentSize = dataIn.readInt();
        maxWindowSize = dataIn.readInt();


        if (senderAckFlag == 1) {
            // System.err.println("Received ACK, so await data transfer.");
            Logger.logData(logStream, "rcv", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                senderSeqNum, senderNumBytes, senderAckNum);
        }

        // RECEIVE FILE -------------------------------------------------------

        File fileReceived = new File(outputFilename);
        FileOutputStream outputStream = new FileOutputStream(fileReceived);

        while (true) {

            // Receive data and write to file.

            receivePacket = 
                 new DatagramPacket(receiveData, receiveData.length);
            receiverSocket.receive(receivePacket);

            currBytes = receivePacket.getData();

            byteIn = new ByteArrayInputStream(currBytes);
            dataIn = new DataInputStream(byteIn);
            senderSeqNum = dataIn.readInt();
            senderAckNum = dataIn.readInt();
            senderAckFlag = dataIn.readByte();
            senderSynFlag = dataIn.readByte();
            senderFinFlag = dataIn.readByte();
            maxSegmentSize = dataIn.readInt();
            maxWindowSize = dataIn.readInt();

            // Get the number of bytes of data in the received packet.
            // This specific number will prevent readNBytes from reading more
            // data from the packet than the packet actually has - a weird bug.
            int dataSize = receivePacket.getLength() - headerSize;

            byte[] fileData = dataIn.readNBytes(dataSize);

            dataIn.close();
            byteIn.close();

            if (senderFinFlag == 1) {
                break;
            }

            senderNumBytes = fileData.length;

            Logger.logData(logStream, "rcv", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "D", 
                senderSeqNum, senderNumBytes, senderAckNum);

            outputStream.write(fileData);
            receivePacket.setLength(receiveData.length);

            // Send ack back.
            receiverSeqNum = senderAckNum;
            receiverAckNum = senderSeqNum + fileData.length;

            byte[] ackData = Helper.makePacketBytes(receiverSeqNum, 
                receiverAckNum, 0, 0, 0, maxSegmentSize, maxWindowSize);

            DatagramPacket ackPacket = 
            new DatagramPacket(ackData, ackData.length, 
                senderHostIP, senderPort);
            receiverSocket.send(ackPacket);
            
            receiverNumBytes = 0;
            Logger.logData(logStream, "snd", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                receiverSeqNum, receiverNumBytes, receiverAckNum);

            
		} // end of while (true)

        // Receive sender's FIN and send out FIN-ACK for it -------------------

        if (senderFinFlag == 1) {

            senderNumBytes = 0;
            Logger.logData(logStream, "rcv", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "F", 
                senderSeqNum, senderNumBytes, senderAckNum);

            receiverSeqNum = senderAckNum;
            receiverAckNum = senderSeqNum + 1;

            byte[] handshakeData = Helper.makePacketBytes(receiverSeqNum, 
                receiverAckNum, 1, 0, 1, maxSegmentSize, maxWindowSize);

            DatagramPacket ackPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                senderHostIP, senderPort);
            receiverSocket.send(ackPacket);

            Logger.logData(logStream, "snd", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "FA", 
                receiverSeqNum, receiverNumBytes, receiverAckNum);
        }

    // Receive ACK --------------------------------------------------------

        receiverSocket.receive(receivePacket);

        currBytes = receivePacket.getData();
        byteIn = new ByteArrayInputStream(currBytes);
        dataIn = new DataInputStream(byteIn);
        senderSeqNum = dataIn.readInt();
        senderAckNum = dataIn.readInt();
        senderAckFlag = dataIn.readByte();
        senderSynFlag = dataIn.readByte();
        senderFinFlag = dataIn.readByte();
        maxSegmentSize = dataIn.readInt();
        maxWindowSize = dataIn.readInt();


        if (senderAckFlag == 1) {
            // System.err.println("Received ACK so close receiver socket.");
            Logger.logData(logStream, "rcv", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                senderSeqNum, senderNumBytes, senderAckNum);
            
            outputStream.close();
            receiverSocket.close();
        }


	} // end of main()

} // end of class 