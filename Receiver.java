import java.io.*;
import java.net.*;
import java.util.*;

public class Receiver {

    private static final int headerSize = 19;
    private static final int maxUDPDatagramSize = 65515;

	public static void main(String[] args) throws Exception {

        // Initialise seq and ack nums for logging.
        int receiverSeqNum = 154;
        int receiverAckNum = 0;
        int receiverNumBytes = 0;
        int senderNumBytes = 0;
        // 1 is added to initSeqNum due to connection establishment
        int expectedSeqNum = Globals.initSeqNum + 1; 
        boolean outOfOrder = false;
        int lastReceivedSeqNum = 0;

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

        // Create packet buffer for receiving data
        ArrayList<Packet> receiveBuffer = new ArrayList<>();

        while (true) {

            // Receiver always sends 0 bytes.
            receiverNumBytes = 0;

            // Print the packets currently in the buffer
            System.err.println("Packets currently in receive buffer:");
            for (Packet bufferPacket : receiveBuffer) {
                System.err.println("\t" + bufferPacket.getSeqNum());
            }

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


            Packet currPacket = new Packet(senderSeqNum, senderAckNum, 
                senderAckFlag, senderSynFlag, senderFinFlag, maxSegmentSize, 
                maxWindowSize, fileData);

            Logger.logData(logStream, "rcv", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "D", 
                currPacket.getSeqNum(), currPacket.getLength(), 
                currPacket.getAckNum());

            // Added code -----------------------------------------------------

            // TODO: add this to summary log stats
            // if (currPacket.getSeqNum() == lastReceivedSeqNum) {
            //     System.err.println("This is entered.");
            //     continue;
            // }

            // lastReceivedSeqNum = currPacket.getSeqNum();

            if (senderFinFlag == 1) {
                break;
            }

            // System.err.println("currPacket.getSeqNum() == " + currPacket.getSeqNum());
            // System.err.println("expectedSeqNum == " + expectedSeqNum);

            if (currPacket.getSeqNum() == expectedSeqNum) {
                currPacket.writeData(outputStream);
                receiverAckNum += currPacket.getLength();
                if (outOfOrder == true) {
                    if (receiveBuffer.size() > 0) {
                        if (receiveBuffer.get(0).getSeqNum() == 
                            (currPacket.getSeqNum() + currPacket.getLength())) {
                            for (Packet bufferPacket: receiveBuffer) {
                                bufferPacket.writeData(outputStream);
                            }
                            receiveBuffer = new ArrayList<>();
                            outOfOrder = false;
                            // Send ACK back.
                            receiverSeqNum = senderAckNum;
                            Packet responsePacket = new Packet(receiverSeqNum, receiverAckNum, 
                                1, 0, 0, maxSegmentSize, maxWindowSize, null);
                            responsePacket.getHeaders();
                            DatagramPacket ackPacket = 
                                responsePacket.createAckPacket(senderHostIP, senderPort);
                            receiverSocket.send(ackPacket);
                            Logger.logData(logStream, "snd", 
                                Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                                receiverSeqNum, receiverNumBytes, receiverAckNum);
                            expectedSeqNum = receiverAckNum;
                        } else {
                            expectedSeqNum += currPacket.getLength();
                            outOfOrder = true;
                            receiverSeqNum = senderAckNum;
                            Packet responsePacket = new Packet(receiverSeqNum, expectedSeqNum, 
                                1, 0, 0, maxSegmentSize, maxWindowSize, null);
                            responsePacket.getHeaders();
                            DatagramPacket ackPacket = 
                                responsePacket.createAckPacket(senderHostIP, senderPort);
                            receiverSocket.send(ackPacket);
                            Logger.logData(logStream, "snd", 
                                Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                                receiverSeqNum, receiverNumBytes, expectedSeqNum);
                        }
                    }
                } else {
                    // System.err.println("This is entered.");
                    receiverSeqNum = senderAckNum;
                    Packet responsePacket = new Packet(receiverSeqNum, receiverAckNum, 
                        1, 0, 0, maxSegmentSize, maxWindowSize, null);
                    responsePacket.getHeaders();
                    DatagramPacket ackPacket = 
                        responsePacket.createAckPacket(senderHostIP, senderPort);
                    receiverSocket.send(ackPacket);
                    Logger.logData(logStream, "snd", 
                        Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                        receiverSeqNum, receiverNumBytes, receiverAckNum);
                    expectedSeqNum = receiverAckNum;
                }
            } else {
                receiveBuffer.add(currPacket);
                receiverAckNum += currPacket.getLength();
                outOfOrder = true;
                Packet responsePacket = new Packet(receiverSeqNum, expectedSeqNum, 
                        1, 0, 0, maxSegmentSize, maxWindowSize, null);
                responsePacket.getHeaders();
                DatagramPacket ackPacket = 
                    responsePacket.createAckPacket(senderHostIP, senderPort);
                receiverSocket.send(ackPacket);
                Logger.logData(logStream, "snd", 
                    Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                    receiverSeqNum, receiverNumBytes, expectedSeqNum);
                expectedSeqNum = receiverAckNum;
            }

            // End of added code ----------------------------------------------

            // Removed code ---------------------------------------------------

            // currPacket.writeData(outputStream);
            // receiverAckNum += fileData.length;

            // // Send ACK back.
            // receiverSeqNum = senderAckNum;

            // Packet responsePacket = new Packet(receiverSeqNum, receiverAckNum, 
            // 1, 0, 0, maxSegmentSize, maxWindowSize, null);
            // responsePacket.getHeaders();

            // DatagramPacket ackPacket = 
            //     responsePacket.createAckPacket(senderHostIP, senderPort);

            // receiverSocket.send(ackPacket);
            
            // Logger.logData(logStream, "snd", 
            //     Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
            //     receiverSeqNum, receiverNumBytes, receiverAckNum);

            // expectedSeqNum = receiverAckNum;

            // End of removed code --------------------------------------------

        
            
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