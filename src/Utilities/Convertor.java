package Utilities;

import java.nio.ByteBuffer;

public class Convertor {
    public static byte[] longToBytes(long arg){
        ByteBuffer buff = ByteBuffer.allocate(Long.SIZE / 8);
        buff.putLong(arg);
        return buff.array();
    }

    public static byte[] intToBytes(int arg){
        ByteBuffer buff = ByteBuffer.allocate(Integer.SIZE / 8);
        buff.putInt(arg);
        return buff.array();
    }

    public static byte[] shortToBytes(short arg){
        ByteBuffer buff = ByteBuffer.allocate(Short.SIZE / 8);
        buff.putShort(arg);
        return buff.array();
    }

    public static long bytesToLong(byte[] arg){
        ByteBuffer buff = ByteBuffer.wrap(arg);
        return buff.getLong();
    }

    public static int bytesToInt(byte[] arg){
        ByteBuffer buff = ByteBuffer.wrap(arg);
        return buff.getInt();
    }

    public static short bytesToShort(byte[] arg){
        ByteBuffer buff = ByteBuffer.wrap(arg);
        return buff.getShort();
    }
}
