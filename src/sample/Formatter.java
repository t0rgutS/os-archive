package sample;

import OSComponents.*;
import Utilities.Convertor;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class Formatter {
    public void formate(short hd_size, Superblock sb, byte[] b_bitm, byte[] i_bitm, Inode[] inodes, Record[] recs,
                        Block[] data) throws IOException {
        try {
            RandomAccessFile raf = new RandomAccessFile("OScore (" + hd_size + ")", "rw");
            /** Формарование файла */
            long file_size = hd_size * 1024 * 1024;
            long part = file_size / 5;
            while(part > Integer.MAX_VALUE)
                part = file_size / 5;
            int not_a_half = (int) part;
            byte[] musor = new byte[not_a_half];
            for(int i = 0; i < musor.length; i++)
                musor[i] = 46;
            int i = 0;
            while(i < file_size){
                raf.write(musor);
                i += not_a_half;
            }
            raf.seek(0);
            raf.write(sb.toBytes());
            System.out.println("Фактический размер суперблока: " + sb.toBytes().length);
            raf.seek(sb.getTo_cl_bitmap());
            raf.write(b_bitm);
            raf.seek(sb.getTo_inode_bitmap());
            raf.write(i_bitm);
            raf.seek(sb.getTo_ilist());
            i = 0;
            while(inodes[i] != null){
                raf.write(inodes[i].toBytes());
                i++;
            }
            System.out.println("Фактический размер inode 1: " + inodes[i - 2].toBytes().length);
            System.out.println("Фактический размер inode 2: " + inodes[i - 1].toBytes().length);
            raf.seek(sb.getTo_root_cat());
           // i = 0;
            for(i = 0; i < 3; i++){
                raf.write(recs[i].toBytes());
            }
           // while (recs[i] != null){

           //     i++;
          //  }
            raf.seek(sb.getTo_data());
            i = 0;
            while(data[i] != null){
                raf.write(data[i].getData());
                i++;
            }
            raf.close();
            /** Чтение строки из файла */
            raf = new RandomAccessFile("OScore (" + hd_size + ")", "r");
            byte[] buff = new byte[Superblock.getSize()];
            raf.read(buff);
            String s = new String(buff);
            System.out.println(s);
            System.out.println("Смещение к корневому каталогу: " + Convertor.bytesToInt(Arrays.copyOfRange(buff, 38, 42)));
            raf.seek(sb.getTo_root_cat());
            buff = new byte[2];
            raf.read(buff);
            short len = Convertor.bytesToShort(buff);
            raf.seek(sb.getTo_root_cat() + len);
            raf.read(buff);
            short newLen = Convertor.bytesToShort(buff);
            raf.seek(sb.getTo_root_cat() + len);
            buff = new byte[newLen];
            raf.read(buff);
            s = new String(buff);
            System.out.println(s);
            System.out.println("Номер inode: " + Convertor.bytesToInt(Arrays.copyOfRange(buff, newLen - 4, newLen)));
            raf.close();

        } catch(IOException ioe){
            ioe.printStackTrace();
        }
    }
}
