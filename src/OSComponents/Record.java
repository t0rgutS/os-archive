package OSComponents;

import Utilities.Convertor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Record {
    String file_name;
    short name_length;
    String file_ext;
    short rec_length;
    int inode_num;

    public static short getSize() {
        return 258 + Integer.SIZE / 8 + Short.SIZE / 8 * 2;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(Superblock.getSize());
        stream.write(Convertor.shortToBytes(rec_length));
        stream.write(Convertor.shortToBytes(name_length));
        if (name_length != -1) {
            stream.write(file_name.getBytes());
            stream.write(file_ext.getBytes());
            stream.write(Convertor.intToBytes(inode_num));
        } else {
            while(stream.toByteArray().length < rec_length)
                stream.write(46);
        }
        return stream.toByteArray();
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public void setName_length() {
        name_length = (short) file_name.getBytes().length;
    }

    public void setName_length(short name_length) {
        this.name_length = name_length;
    }

    public void setRec_length(short rec_length) {
        this.rec_length = rec_length;
    }

    public void setFile_ext(String file_ext) {
        this.file_ext = file_ext;
    }

    public void setRec_length() {
        this.rec_length = (short) (file_name.getBytes().length + 3 + Short.SIZE / 8 * 2 + Integer.SIZE / 8);
    }

    public void setInode_num(int inode_num) {
        this.inode_num = inode_num;
    }

    public String getFile_ext() {
        return file_ext;
    }

    public String getFile_name() {
        return file_name;
    }

    public int getInode_num() {
        return inode_num;
    }

    public short getName_length() {
        return name_length;
    }

    public short getRec_length() {
        return rec_length;
    }
}
