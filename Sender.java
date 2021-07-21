import java.io.*;
import java.net.*;

public class Sender {

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
		
		// create socket which connects to receiver
		DatagramSocket clientSocket = new DatagramSocket(0);
        /*
        This line creates the client’s socket, called clientSocket. 
        DatagramSocket indicates that we are using UDP.
        */
        
		// get input from file

        File fileToSend = new File(filename);

        FileInputStream inFromFile = new FileInputStream(fileToSend);

        //prepare for sending
        byte[] sendData = new byte[64];

        int i = 1;

        while ((inFromFile.read(sendData)) != -1) {

            // write to receiver, need to create DatagramPacket with receiver 
            // address and port No
            DatagramPacket sendPacket = 
                new DatagramPacket(sendData, sendData.length, receiverHostIP, 
                    receiverPort);
            //actual send call
            clientSocket.send(sendPacket);
            String currLine = new String(sendData);
            System.err.print("Sent string " + i + ": " + currLine);
            i++;
        }
        
        //close the scoket
        clientSocket.close();
        inFromFile.close();
		
	} // end of main

    // private static void senderHandshake(InetAddress receiverHostIP, 
    //     int receiverPort, DatagramSocket clientSocket) {
    //     byte[] sendData = new byte[1024];
    //     sendData = "SYN".getBytes();
    //     System.err.println("Sending SYN.");
    //     DatagramPacket sendPacket = 
    //         new DatagramPacket(sendData, sendData.length, receiverHostIP, 
    //             receiverPort);
    //     //actual send call
    //     try {
    //         clientSocket.send(sendPacket);
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }

    //     //prepare buffer to receive reply
    //     byte[] receiveData=new byte[1024];
    //     // receive from receiver
    //     DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
    //     try {
    //         clientSocket.receive(receivePacket);
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }

    //     String receivedSentence = new String(receivePacket.getData());
    //     if (receivedSentence.equals("SYNACK")) {
    //         sendData = "ACK".getBytes();
    //         System.err.println("Got SYNACK so sending out ACK.");
    //         sendPacket = 
    //         new DatagramPacket(sendData, sendData.length, receiverHostIP, 
    //             receiverPort);
    //         try {
    //             clientSocket.send(sendPacket);
    //         } catch (IOException e) {
    //             e.printStackTrace();
    //         }
    //     }

    // }

} // end of class Sender

