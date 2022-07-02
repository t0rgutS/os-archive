package OSComponents;

import Utilities.Convertor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Inode {
    short type_rights;
    byte uid;
    byte gid;
    int file_size;
    long create_date;
    long modif_date;
    short block_count;
    int[] addr;
    int indir_addr;
    int num;    //Для операций с файлами

    public Inode() {
        addr = new int[12];
        for (int i = 0; i < 12; i++)
            addr[i] = -1;
        indir_addr = -1;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public int getNum() {
        return num;
    }

    public static short getSize() {
        return 2 + Integer.SIZE / 8 * 14 + Short.SIZE / 8 * 2 + Long.SIZE / 8 * 2;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(Superblock.getSize());
        stream.write(Convertor.intToBytes(file_size));
        stream.write(Convertor.shortToBytes(type_rights));
        stream.write((char) uid);
        stream.write((char) gid);
        stream.write(Convertor.longToBytes(create_date));
        stream.write(Convertor.longToBytes(modif_date));
        stream.write(Convertor.shortToBytes(block_count));
        for (int i = 0; i < addr.length; i++)
            stream.write(Convertor.intToBytes(addr[i]));
        stream.write(Convertor.intToBytes(indir_addr));
        return stream.toByteArray();
    }

    public void setBlock_count(short block_count) {
        this.block_count = block_count;
    }

    public void setAddr(int ind, int val) {
        this.addr[ind] = val;
    }

    public void setCreate_date(long create_date) {
        this.create_date = create_date;
    }

    public void setFile_size(int file_size) {
        this.file_size = file_size;
    }

    public void setGid(byte gid) {
        this.gid = gid;
    }

    public void setIndir_addr(int indir_addr) {
        this.indir_addr = indir_addr;
    }

    public void setModif_date(long modif_date) {
        this.modif_date = modif_date;
    }

    public void setType_rights(short type_rights) {
        this.type_rights = type_rights;
    }

    public void setUid(byte uid) {
        this.uid = uid;
    }

    public byte getGid() {
        return gid;
    }

    public short getType_rights() {
        return type_rights;
    }

    public byte getUid() {
        return uid;
    }

    public int getFile_size() {
        return file_size;
    }

    public int getIndir_addr() {
        return indir_addr;
    }

    public int getAddr(int ind) {
        return addr[ind];
    }

    public long getCreate_date() {
        return create_date;
    }

    public long getModif_date() {
        return modif_date;
    }

    public short getBlock_count() {
        return block_count;
    }
}
