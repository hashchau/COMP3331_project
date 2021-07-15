import java.io.*;
import java.net.*;

public class Receiver {

	public static void main(String[] args)throws Exception {
        /* define socket parameters, Address + PortNo, Address will default to localhost */
		int receiverPort = Integer.parseInt(args[0]);
        String outputFilename = args[1];
		/* change above port number if required */
		
		/*create receiver socket that is assigned the receiverPort (6789)
        We will listen on this port for requests from clients
         DatagramSocket specifies that we are using UDP */
		DatagramSocket receiverSocket = new DatagramSocket(receiverPort);
        System.out.println("receiver is ready :");
        
        //prepare buffers
        byte[] receiveData = new byte[64];
        // byte[] sendData = new byte[1024];

        File fileReceived = new File(outputFilename);
        FileOutputStream outputStream = new FileOutputStream(fileReceived);
		
        while (true){
            //receive UDP datagram
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            receiverSocket.receive(receivePacket);
           
            //get info of the client with whom we are communicating
            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();
            

            String currString = new String(receivePacket.getData());
            System.err.print("Current string is: " + currString + "\n");

            outputStream.write(receivePacket.getData());

            //send it back to client
            // DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            // receiverSocket.send(sendPacket);
            // System.err.println("Sending back: [" + sentence + "]");
            

		} // end of while (true)

	} // end of main()

} // end of class 