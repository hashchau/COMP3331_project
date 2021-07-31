import java.io.*;
import java.util.*;
import java.net.*;

public class Packet {

    private int seqNum;
    private int ackNum;
    private int ackFlag;
    private int synFlag;
    private int finFlag;
    private int maxSegmentSize;
    private int maxWindowSize;
    private byte[] data = null;

    public Packet(int seqNum, int ackNum, int ackFlag, int synFlag, int finFlag, int maxSegmentSize, int maxWindowSize, byte[] data) {
        this.seqNum = seqNum;
        this.ackNum = ackNum;
        this.ackFlag = ackFlag;
        this.synFlag = synFlag;
        this.finFlag = finFlag;
        this.maxSegmentSize = maxSegmentSize;
        this.maxWindowSize = maxWindowSize;
        this.data = data;
    }

    private void getData() throws IOException {

        Globals.bytesRead = Globals.inFromFile.read(Globals.fileData);

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        dataOut.writeInt(this.seqNum); // Sequence number
        dataOut.writeInt(this.ackNum); // ACK number
        dataOut.writeByte(this.ackFlag); // ACK flag
        dataOut.writeByte(this.synFlag); // SYN flag
        dataOut.writeByte(this.finFlag); // FIN flag
        dataOut.writeInt(this.maxSegmentSize); // MSS
        dataOut.writeInt(this.maxWindowSize); // MWS
        dataOut.write(Globals.fileData, 0, Globals.bytesRead);

        this.data = byteOut.toByteArray();

        Globals.sumBytesRead += Globals.bytesRead;
    }

    private DatagramPacket createDatagramPacket() {
        DatagramPacket sendPacket = 
        new DatagramPacket(this.data, this.data.length, 
            Globals.receiverHostIP, Globals.receiverPort);
        return sendPacket;
    }

}
