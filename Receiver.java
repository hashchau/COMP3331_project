import java.io.*;
import java.net.*;

public class Receiver {

    private static final int HEADERSIZE = 18;

	public static void main(String[] args) throws Exception {
		int receiverPort = Integer.parseInt(args[0]);
        String outputFilename = args[1];

		DatagramSocket receiverSocket = new DatagramSocket(receiverPort);
        System.out.println("Receiver is ready:");
        
        // Receive SYN and send out SYN-ACK -----------------------------------

        byte[] receiveData = new byte[64];

        DatagramPacket receivePacket = 
            new DatagramPacket(receiveData, receiveData.length);
        receiverSocket.receive(receivePacket);
       
        // Get info of the client with whom we are communicating
        InetAddress clientHostIP = receivePacket.getAddress();
        int clientPort = receivePacket.getPort();

        byte[] currBytes = receivePacket.getData();
        ByteArrayInputStream byteIn = new ByteArrayInputStream(currBytes);
        DataInputStream dataIn = new DataInputStream(byteIn);
        int seqNum = dataIn.readInt();
        int ackNum = dataIn.readInt();
        int ackFlag = dataIn.readByte();
        int synFlag = dataIn.readByte();
        int finFlag = dataIn.readByte();
        int maxSegmentSize = dataIn.readInt();
        int maxWindowSize = dataIn.readInt();

        // Helper.printHeader(seqNum, ackNum, ackFlag, synFlag, finFlag, 
        //     maxSegmentSize, maxWindowSize);

        if (synFlag == 1) {
            System.err.println("Received SYN, so sending out SYN-ACK.");

            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(byteOut);
            dataOut.writeInt(0); // Sequence number
            dataOut.writeInt(1); // ACK number
            dataOut.writeByte(1); // ACK flag
            dataOut.writeByte(1); // SYN flag
            dataOut.writeByte(0); // FIN flag
            dataOut.writeInt(maxSegmentSize); // MSS
            dataOut.writeInt(maxWindowSize); // MWS
            
            byte[] handshakeData = byteOut.toByteArray();
            DatagramPacket ackPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                clientHostIP, clientPort);
                receiverSocket.send(ackPacket);
        }

        // Receive ACK --------------------------------------------------------

        receiverSocket.receive(receivePacket);

        currBytes = receivePacket.getData();
        byteIn = new ByteArrayInputStream(currBytes);
        dataIn = new DataInputStream(byteIn);
        seqNum = dataIn.readInt();
        ackNum = dataIn.readInt();
        ackFlag = dataIn.readByte();
        synFlag = dataIn.readByte();
        finFlag = dataIn.readByte();
        maxSegmentSize = dataIn.readInt();
        maxWindowSize = dataIn.readInt();

        // Helper.printHeader(seqNum, ackNum, ackFlag, synFlag, finFlag, 
        //     maxSegmentSize, maxWindowSize);

        if (ackFlag == 1) {
            System.err.println("Received ACK, so await data transfer.");
        }

        // Receive file -------------------------------------------------------

        // File fileReceived = new File(outputFilename);
        // FileOutputStream outputStream = new FileOutputStream(fileReceived);
        // int i = 1;
        // while (true){
        //     //receive UDP datagram
        //     DatagramPacket receivePacket = 
        //          new DatagramPacket(receiveData, receiveData.length);
        //     receiverSocket.receive(receivePacket);
        //     String currString = new String(receivePacket.getData());
        //     System.err.print("Received string " + i + ": " + currString);
        //     outputStream.write(receivePacket.getData());
        //     receivePacket.setLength(receiveData.length);
        //     i++;
		// } // end of while (true)

        // Receive client's FIN and send out ACK for it -----------------------

        receiverSocket.receive(receivePacket);

        currBytes = receivePacket.getData();
        byteIn = new ByteArrayInputStream(currBytes);
        dataIn = new DataInputStream(byteIn);
        seqNum = dataIn.readInt();
        ackNum = dataIn.readInt();
        ackFlag = dataIn.readByte();
        synFlag = dataIn.readByte();
        finFlag = dataIn.readByte();
        maxSegmentSize = dataIn.readInt();
        maxWindowSize = dataIn.readInt();

        // Helper.printHeader(seqNum, ackNum, ackFlag, synFlag, finFlag, 
        //     maxSegmentSize, maxWindowSize);

        if (finFlag == 1) {
            System.err.println("Received client's FIN, so sending out ACK.");

            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(byteOut);
            dataOut.writeInt(0); // Sequence number
            dataOut.writeInt(2); // ACK number
            dataOut.writeByte(1); // ACK flag
            dataOut.writeByte(0); // SYN flag
            dataOut.writeByte(0); // FIN flag
            dataOut.writeInt(maxSegmentSize); // MSS
            dataOut.writeInt(maxWindowSize); // MWS
            
            byte[] handshakeData = byteOut.toByteArray();
            DatagramPacket ackPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                clientHostIP, clientPort);
                receiverSocket.send(ackPacket);
        }


        // Send out FIN -------------------------------------------------------

        System.err.println("Sending out FIN.");

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        dataOut.writeInt(0); // Sequence number
        dataOut.writeInt(2); // ACK number
        dataOut.writeByte(0); // ACK flag
        dataOut.writeByte(0); // SYN flag
        dataOut.writeByte(1); // FIN flag
        dataOut.writeInt(maxSegmentSize); // MSS
        dataOut.writeInt(maxWindowSize); // MWS
        
        byte[] handshakeData = byteOut.toByteArray();
        DatagramPacket ackPacket = 
        new DatagramPacket(handshakeData, handshakeData.length, 
            clientHostIP, clientPort);
            receiverSocket.send(ackPacket);

        // Receive ACK --------------------------------------------------------

        receiverSocket.receive(receivePacket);

        currBytes = receivePacket.getData();
        byteIn = new ByteArrayInputStream(currBytes);
        dataIn = new DataInputStream(byteIn);
        seqNum = dataIn.readInt();
        ackNum = dataIn.readInt();
        ackFlag = dataIn.readByte();
        synFlag = dataIn.readByte();
        finFlag = dataIn.readByte();
        maxSegmentSize = dataIn.readInt();
        maxWindowSize = dataIn.readInt();

        // Helper.printHeader(seqNum, ackNum, ackFlag, synFlag, finFlag, 
        //     maxSegmentSize, maxWindowSize);

        if (ackFlag == 1) {
            System.err.println("Received ACK so close receiver socket.");
            receiverSocket.close();
        }


	} // end of main()

} // end of class 