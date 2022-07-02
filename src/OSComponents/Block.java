package OSComponents;

import java.util.Arrays;

public class Block {
    private byte[] data;

    public Block(short clust_size) {
        data = new byte[clust_size];
        for (int i = 0; i < clust_size; i++)
            data[i] = 46;
    }

    public byte[] getSome(int to) {
        return Arrays.copyOfRange(data, 0, to);
    }

    public void clear(){
        for(int i = 0; i < data.length; i++)
            data[i] = 46;
    }

    public void write(byte[] val, int offset) {
        int i = offset, k = 0;
        while (k < val.length) {
            data[i] = val[k];
            k++;
            i++;
        }
    }

    public byte[] getData() {
        return data;
    }
}
