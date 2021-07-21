import java.nio.*;
public class Helper {
    public static byte[] intToBytes(int i) {
        ByteBuffer currBuffer = ByteBuffer.allocate(4);
        currBuffer.putInt(i);
        return currBuffer.array();
    }

    public static int byteArrayToInt(byte[] intBytes) {
        ByteBuffer currBuffer = ByteBuffer.wrap(intBytes);
        return currBuffer.getInt();
    }
}
