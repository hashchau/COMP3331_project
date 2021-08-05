import java.io.*;
import java.net.*;
import java.util.*;

public class Receiver {

    private static final int headerSize = 19;
    private static final int maxUDPDatagramSize = 65515;

	public static void main(String[] args) throws Exception {

        // Initialise seq and ack nums for logging.
        int receiverSeqNum = 0;
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
        
        // Receive SYN and send out SYN-ACK -----------------------------------

        byte[] receiveData = new byte[maxUDPDatagramSize];

        DatagramPacket receivePacket = 
            new DatagramPacket(receiveData, receiveData.length);
        receiverSocket.receive(receivePacket);
       
        // Get info of the sender with whom we are communicating
        InetAddress senderHostIP = receivePacket.getAddress();
        int senderPort = receivePacket.getPort();

        // Get field values from received packet
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

        // Calculate packet size from given MSS and designed header size
        int packetSize = headerSize + maxSegmentSize;
        
        // Make data buffer for received packets the correct size
        receiveData = new byte[packetSize];

        receiverAckNum = senderSeqNum + 1;

        // SYN received so send SYN-ACK
        if (senderSynFlag == 1) {
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

        // Get field values from received packet
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

        // Received ACK so finish connection establishment and wait to receive file
        if (senderAckFlag == 1) {
            Logger.logData(logStream, "rcv", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                senderSeqNum, senderNumBytes, senderAckNum);
        }

        // RECEIVE FILE -------------------------------------------------------

        // Create an output stream to write the received data into
        File fileReceived = new File(outputFilename);
        FileOutputStream outputStream = new FileOutputStream(fileReceived);

        // Create packet buffer for receiving data
        ArrayList<Packet> receiveBuffer = new ArrayList<>();

        // Keep looping and getting data packets from the sender until a FIN packet is received
        while (true) {

            // Receiver always sends 0 bytes
            receiverNumBytes = 0;

            receivePacket = 
                 new DatagramPacket(receiveData, receiveData.length);
                 
            receiverSocket.receive(receivePacket);

            // Get field values from received packet
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
            // data from the packet than the packet actually has.
            int dataSize = receivePacket.getLength() - headerSize;

            byte[] fileData = dataIn.readNBytes(dataSize);

            // Create Packet object to contain received DatagramPacket from sender
            Packet currPacket = new Packet(senderSeqNum, senderAckNum, 
                senderAckFlag, senderSynFlag, senderFinFlag, maxSegmentSize, 
                maxWindowSize, fileData, System.nanoTime());

            Logger.logData(logStream, "rcv", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "D", 
                currPacket.getSeqNum(), currPacket.getLength(), 
                currPacket.getAckNum());

            // If the current, received packet has the same sequence number as the last received
            // packet, then discard it
            if (currPacket.getSeqNum() == lastReceivedSeqNum) {
                Globals.totalDupSegmentsReceived++;
                continue;
            }

            lastReceivedSeqNum = currPacket.getSeqNum();

            // Break the loop if a packet with a FIN flag is received
            if (senderFinFlag == 1) {
                break;
            }

            // If the received packet arrives in order, then write it straight to the output file
            if (currPacket.getSeqNum() == expectedSeqNum) {
                currPacket.writeData(outputStream);
                receiverAckNum += currPacket.getLength();
                // If the packets in the receive buffer are still out of order, process further
                if (outOfOrder == true) {
                    if (receiveBuffer.size() > 0) {
                        // If packets in buffer are in order, write those to output file as well
                        // and send an ACK
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
                                1, 0, 0, maxSegmentSize, maxWindowSize, null, System.nanoTime());
                            responsePacket.getHeaders();
                            DatagramPacket ackPacket = 
                                responsePacket.createAckPacket(senderHostIP, senderPort);
                            receiverSocket.send(ackPacket);
                            Logger.logData(logStream, "snd", 
                                Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                                receiverSeqNum, receiverNumBytes, receiverAckNum);
                            expectedSeqNum = receiverAckNum;
                        // If buffer packets are still not in order, send more duplicate ACKs with
                        // the expected sequence number
                        } else {
                            expectedSeqNum += currPacket.getLength();
                            outOfOrder = true;
                            receiverSeqNum = senderAckNum;
                            Packet responsePacket = new Packet(receiverSeqNum, expectedSeqNum, 
                                1, 0, 0, maxSegmentSize, maxWindowSize, null, System.nanoTime());
                            responsePacket.getHeaders();
                            DatagramPacket ackPacket = 
                                responsePacket.createAckPacket(senderHostIP, senderPort);
                            receiverSocket.send(ackPacket);
                            Logger.logData(logStream, "snd", 
                                Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                                receiverSeqNum, receiverNumBytes, expectedSeqNum);
                        }
                    }
                // If packets are in order, reply with an ACK
                } else {
                    receiverSeqNum = senderAckNum;
                    Packet responsePacket = new Packet(receiverSeqNum, receiverAckNum, 
                        1, 0, 0, maxSegmentSize, maxWindowSize, null, System.nanoTime());
                    responsePacket.getHeaders();
                    DatagramPacket ackPacket = 
                        responsePacket.createAckPacket(senderHostIP, senderPort);
                    receiverSocket.send(ackPacket);
                    Logger.logData(logStream, "snd", 
                        Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                        receiverSeqNum, receiverNumBytes, receiverAckNum);
                    expectedSeqNum = receiverAckNum;
                }
            // If current packet is received out of order, send a duplicate ACK
            } else {
                receiveBuffer.add(currPacket);
                receiverAckNum += currPacket.getLength();
                outOfOrder = true;
                Packet responsePacket = new Packet(receiverSeqNum, expectedSeqNum, 
                        1, 0, 0, maxSegmentSize, maxWindowSize, null, System.nanoTime());
                responsePacket.getHeaders();
                DatagramPacket ackPacket = 
                    responsePacket.createAckPacket(senderHostIP, senderPort);
                receiverSocket.send(ackPacket);
                Logger.logData(logStream, "snd", 
                    Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                    receiverSeqNum, receiverNumBytes, expectedSeqNum);
            }

            
		} // end of while (true)

        // Receive sender's FIN and send out FIN-ACK for it -------------------

        // Received FIN packet so send FIN-ACK packet
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

        // Get field values from received packet
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

        // Received final ACK from sender so finish connection teardown and end the program
        if (senderAckFlag == 1) {
            Logger.logData(logStream, "rcv", 
                Helper.elapsedTimeInMillis(start, System.nanoTime()), "A", 
                senderSeqNum, senderNumBytes, senderAckNum);

            Logger.logReceiverStats(logStream);
            
            outputStream.close();
            receiverSocket.close();
        }

	} // end of main()

} // end of class 