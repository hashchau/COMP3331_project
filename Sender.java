import java.io.*;
import java.net.*;

public class Sender {
    private static final int HEADERSIZE = 18;
	public static void main(String[] args) throws Exception {


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
		
		// create socket, using any port, which connects to receiver
		DatagramSocket clientSocket = new DatagramSocket(0);

        // Send out SYN -------------------------------------------------------

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        dataOut.writeInt(0); // ISN
        dataOut.writeInt(0); // ACK number
        dataOut.writeByte(0); // ACK flag
        dataOut.writeByte(1); // SYN flag
        dataOut.writeByte(0); // FIN flag
        dataOut.writeInt(maxSegmentSize); // MSS
        dataOut.writeInt(maxWindowSize); // MWS
        
        byte[] handshakeData = byteOut.toByteArray();
        
        DatagramPacket synPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                receiverHostIP, receiverPort);
        clientSocket.send(synPacket);

        // Receive SYN-ACK and send out ACK -----------------------------------

        byte[] receiveData = new byte[64];

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);

        byte[] currBytes = receivePacket.getData();
        ByteArrayInputStream byteIn = new ByteArrayInputStream(currBytes);
        DataInputStream dataIn = new DataInputStream(byteIn);
        int seqNum = dataIn.readInt();
        int ackNum = dataIn.readInt();
        int ackFlag = dataIn.readByte();
        int synFlag = dataIn.readByte();
        int finFlag = dataIn.readByte(); 
        maxSegmentSize = dataIn.readInt();
        maxWindowSize = dataIn.readInt();
        
        Helper.printHeader(seqNum, ackNum, ackFlag, synFlag, finFlag, 
            maxSegmentSize, maxWindowSize);

        if (synFlag == 1 && ackFlag == 1) {
            System.err.println("Received SYN-ACK, so sending out ACK.");

            byteOut = new ByteArrayOutputStream();
            dataOut = new DataOutputStream(byteOut);
            dataOut.writeInt(1); // Sequence number
            dataOut.writeInt(0); // ACK number
            dataOut.writeByte(1); // ACK flag
            dataOut.writeByte(0); // SYN flag
            dataOut.writeByte(0); // FIN flag
            dataOut.writeInt(maxSegmentSize); // MSS
            dataOut.writeInt(maxWindowSize); // MWS

            handshakeData = byteOut.toByteArray();
            DatagramPacket ackPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                receiverHostIP, receiverPort);
                clientSocket.send(ackPacket);
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
        //     clientSocket.send(sendPacket);
        //     String currLine = new String(sendData);
        //     System.err.print("Sent string " + i + ": " + currLine);
        //     i++;
        // }

        // File transferred so send out FIN -----------------------------------

        System.err.println("Data transferred, so sending out FIN.");

        byteOut = new ByteArrayOutputStream();
        dataOut = new DataOutputStream(byteOut);
        dataOut.writeInt(1); // Sequence number
        dataOut.writeInt(0); // ACK number
        dataOut.writeByte(0); // ACK flag
        dataOut.writeByte(0); // SYN flag
        dataOut.writeByte(1); // FIN flag
        dataOut.writeInt(maxSegmentSize); // MSS
        dataOut.writeInt(maxWindowSize); // MWS

        handshakeData = byteOut.toByteArray();
        DatagramPacket finPacket = 
        new DatagramPacket(handshakeData, handshakeData.length, 
            receiverHostIP, receiverPort);
            clientSocket.send(finPacket);

        // Receive ACK and wait for server's FIN ------------------------------

        receiveData = new byte[64];

        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);

        currBytes = receivePacket.getData();
        byteIn = new ByteArrayInputStream(currBytes);
        dataIn = new DataInputStream(byteIn);
        seqNum = dataIn.readInt();
        ackNum = dataIn.readInt();
        ackFlag = dataIn.readByte();
        synFlag = dataIn.readByte();
        finFlag = dataIn.readByte(); 
        
        Helper.printHeader(seqNum, ackNum, ackFlag, synFlag, finFlag, 
            maxSegmentSize, maxWindowSize);

        if (ackFlag == 1) {
            System.err.println("Received ACK so wait for server's FIN.");
        }

        // Receive server's FIN and send out ACK ------------------------------
        
        receiveData = new byte[64];

        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);

        currBytes = receivePacket.getData();
        byteIn = new ByteArrayInputStream(currBytes);
        dataIn = new DataInputStream(byteIn);
        seqNum = dataIn.readInt();
        ackNum = dataIn.readInt();
        ackFlag = dataIn.readByte();
        synFlag = dataIn.readByte();
        finFlag = dataIn.readByte(); 
        
        Helper.printHeader(seqNum, ackNum, ackFlag, synFlag, finFlag, 
            maxSegmentSize, maxWindowSize);

        if (finFlag == 1) {
            System.err.println("Received server's FIN so sending out ACK.");

            byteOut = new ByteArrayOutputStream();
            dataOut = new DataOutputStream(byteOut);
            dataOut.writeInt(2); // Sequence number
            dataOut.writeInt(0); // ACK number
            dataOut.writeByte(1); // ACK flag
            dataOut.writeByte(0); // SYN flag
            dataOut.writeByte(0); // FIN flag
            dataOut.writeInt(maxSegmentSize); // MSS
            dataOut.writeInt(maxWindowSize); // MWS
    
            handshakeData = byteOut.toByteArray();
            DatagramPacket ackPacket = 
            new DatagramPacket(handshakeData, handshakeData.length, 
                receiverHostIP, receiverPort);
                clientSocket.send(ackPacket);
        }


        // Close the socket ---------------------------------------------------

        clientSocket.close();

        // inFromFile.close();
		
	} // end of main

} // end of class Sender

