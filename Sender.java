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
		
		BufferedReader inFromFile =
			new BufferedReader(new FileReader(fileToSend));
        
        //prepare for sending
        byte[] sendData = new byte[1024];
        String firstLine = inFromFile.readLine();
        sendData = firstLine.getBytes(); 
		// write to receiver, need to create DatagramPAcket with receiver 
        // address and port No
        DatagramPacket sendPacket = 
            new DatagramPacket(sendData, sendData.length, receiverHostIP, 
                receiverPort);

        //actual send call
        clientSocket.send(sendPacket);
        
        //prepare buffer to receive reply
        byte[] receiveData=new byte[1024];
		// receive from receiver
        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
        clientSocket.receive(receivePacket);
        
        String modifiedSentence = new String(receivePacket.getData());
        System.out.println("FROM receiver:" + modifiedSentence);
        //close the scoket
        clientSocket.close();

        inFromFile.close();
		
	} // end of main

} // end of class UDPClient