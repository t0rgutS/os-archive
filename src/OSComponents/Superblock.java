package OSComponents;

import Utilities.Convertor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Superblock {
    char[] fs_name = {'S', 'a', 'P', 'r', 'P', 'O'};
    int cl_count;
    short cl_size;
    int inode_count;
    short inode_size;
    int free_blocks_count;
    int free_inodes_count;
    int to_cl_bitmap;
    int to_inode_bitmap;
    int to_ilist;
    int to_root_cat;
    int to_data;

    public static short getSize() {
        return 6 + Integer.SIZE / 8 * 9 + Short.SIZE / 8 * 2;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(Superblock.getSize());
        stream.write(new String(fs_name).getBytes(StandardCharsets.US_ASCII));
        stream.write(Convertor.intToBytes(cl_count));
        stream.write(Convertor.shortToBytes(cl_size));
        stream.write(Convertor.intToBytes(inode_count));
        stream.write(Convertor.shortToBytes(inode_size));
        stream.write(Convertor.intToBytes(free_blocks_count));
        stream.write(Convertor.intToBytes(free_inodes_count));
        stream.write(Convertor.intToBytes(to_cl_bitmap));
        stream.write(Convertor.intToBytes(to_inode_bitmap));
        stream.write(Convertor.intToBytes(to_ilist));
        stream.write(Convertor.intToBytes(to_root_cat));
        stream.write(Convertor.intToBytes(to_data));
        return stream.toByteArray();
    }

    public void setCl_count(int cl_count) {
        this.cl_count = cl_count;
    }

    public void setCl_size(short cl_size) {
        this.cl_size = cl_size;
    }

    public void setFree_blocks_count(int free_blocks_count) {
        this.free_blocks_count = free_blocks_count;
    }

    public void setFree_inodes_count(int free_inodes_count) {
        this.free_inodes_count = free_inodes_count;
    }

    public void setInode_count(int inode_count) {
        this.inode_count = inode_count;
    }

    public void setInode_size(short inode_size) {
        this.inode_size = inode_size;
    }

    public void setTo_cl_bitmap(int to_cl_bitmap) {
        this.to_cl_bitmap = to_cl_bitmap;
    }

    public void setTo_data(int to_data) {
        this.to_data = to_data;
    }

    public void setTo_ilist(int to_ilist) {
        this.to_ilist = to_ilist;
    }

    public void setTo_inode_bitmap(int to_inode_bitmap) {
        this.to_inode_bitmap = to_inode_bitmap;
    }

    public void setTo_root_cat(int to_root_cat) {
        this.to_root_cat = to_root_cat;
    }

    public int getCl_count() {
        return cl_count;
    }

    public int getFree_blocks_count() {
        return free_blocks_count;
    }

    public int getFree_inodes_count() {
        return free_inodes_count;
    }

    public int getInode_count() {
        return inode_count;
    }

    public int getTo_cl_bitmap() {
        return to_cl_bitmap;
    }

    public int getTo_data() {
        return to_data;
    }

    public int getTo_ilist() {
        return to_ilist;
    }

    public int getTo_inode_bitmap() {
        return to_inode_bitmap;
    }

    public int getTo_root_cat() {
        return to_root_cat;
    }

    public short getCl_size() {
        return cl_size;
    }

    public short getInode_size() {
        return inode_size;
    }
}
