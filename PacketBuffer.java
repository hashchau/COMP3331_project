import java.io.*;
import java.util.*;
import java.net.*;

public class PacketBuffer {
    private ArrayList<Packet> packetBuffer;


    public PacketBuffer() {
        this.packetBuffer = new ArrayList<>();
    }


    public void addPacket(Packet currPacket) {
        this.packetBuffer.add(currPacket);
    }

    public void removePacket(Packet currPacket) {
        this.packetBuffer.remove(currPacket);
    }

}
