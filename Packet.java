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

    public Packet(int seqNum, int ackNum, int ackFlag, int synFlag, int finFlag, 
        int maxSegmentSize, int maxWindowSize, byte[] data) {
        this.seqNum = seqNum;
        this.ackNum = ackNum;
        this.ackFlag = ackFlag;
        this.synFlag = synFlag;
        this.finFlag = finFlag;
        this.maxSegmentSize = maxSegmentSize;
        this.maxWindowSize = maxWindowSize;
        this.data = data;
    }

    public void getData() throws IOException {

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

    public void getHeaders() throws IOException {

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        dataOut.writeInt(this.seqNum); // Sequence number
        dataOut.writeInt(this.ackNum); // ACK number
        dataOut.writeByte(this.ackFlag); // ACK flag
        dataOut.writeByte(this.synFlag); // SYN flag
        dataOut.writeByte(this.finFlag); // FIN flag
        dataOut.writeInt(this.maxSegmentSize); // MSS
        dataOut.writeInt(this.maxWindowSize); // MWS

        this.data = byteOut.toByteArray();

    }

    public DatagramPacket createDatagramPacket() {
        DatagramPacket sendPacket = 
        new DatagramPacket(this.data, this.data.length, 
            Globals.receiverHostIP, Globals.receiverPort);
        return sendPacket;
    }

    public DatagramPacket createAckPacket(InetAddress senderHostIP, int senderPort) {
        DatagramPacket sendPacket = 
        new DatagramPacket(this.data, this.data.length, 
            senderHostIP, senderPort);
        return sendPacket;
    }

    public int getDataLength() {
        return (this.data.length - Globals.HEADER_SIZE);
    }

    public int getLength() {
        return this.data.length;
    }

    public void writeData(FileOutputStream outputStream) {
        try {
            outputStream.write(this.data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public int getSeqNum() {
        return this.seqNum;
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public int getAckNum() {
        return this.ackNum;
    }

    public void setAckNum(int ackNum) {
        this.ackNum = ackNum;
    }

    public int getAckFlag() {
        return this.ackFlag;
    }

    public void setAckFlag(int ackFlag) {
        this.ackFlag = ackFlag;
    }

    public int getSynFlag() {
        return this.synFlag;
    }

    public void setSynFlag(int synFlag) {
        this.synFlag = synFlag;
    }

    public int getFinFlag() {
        return this.finFlag;
    }

    public void setFinFlag(int finFlag) {
        this.finFlag = finFlag;
    }

    public int getMaxSegmentSize() {
        return this.maxSegmentSize;
    }

    public void setMaxSegmentSize(int maxSegmentSize) {
        this.maxSegmentSize = maxSegmentSize;
    }

    public int getMaxWindowSize() {
        return this.maxWindowSize;
    }

    public void setMaxWindowSize(int maxWindowSize) {
        this.maxWindowSize = maxWindowSize;
    }
    public void setData(byte[] data) {
        this.data = data;
    }


}
