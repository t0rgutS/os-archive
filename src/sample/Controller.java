package sample;

import OSComponents.*;
import Utilities.Convertor;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.input.MouseEvent;
import Utilities.MD5;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Date;

public class Controller {
    @FXML
    ComboBox disk_choose, clus_choose, file_choose;
    @FXML
    Button formate_button, ok_button, go_to_formate;
    @FXML
    AnchorPane panel1, panel2;
    Formatter form;
    Superblock sb;
    Inode[] ilist;
    Block[] blocks;
    Record[] recs;
    byte[] inode_bitmap, block_bitmap;

    void to_Next(String file_name, Stage cur_stage) throws Exception {
        if (sb == null)
            fill_superblock(file_choose.getValue().toString());
        cur_stage.close();
        Stage stage = new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("mainform.fxml"));
        Parent root = loader.load();
        stage.setScene(new Scene(root, 600, 400));
        stage.setX(50);
        stage.show();
        MainFormController mfc = loader.getController();
        mfc.setFile_name(file_name, sb);
    }

    void fill_superblock(String file_name) throws IOException {
        sb = new Superblock();
        RandomAccessFile raf = new RandomAccessFile(file_name, "r");
        byte[] buff = new byte[Superblock.getSize()];
        raf.read(buff);
        raf.close();
        sb.setCl_count(Convertor.bytesToInt(Arrays.copyOfRange(buff, 6, 10)));
        sb.setCl_size(Convertor.bytesToShort(Arrays.copyOfRange(buff, 10, 12)));
        sb.setInode_count(Convertor.bytesToInt(Arrays.copyOfRange(buff, 12, 16)));
        sb.setInode_size(Convertor.bytesToShort(Arrays.copyOfRange(buff, 16, 18)));
        sb.setFree_blocks_count(Convertor.bytesToInt(Arrays.copyOfRange(buff, 18, 22)));
        sb.setFree_inodes_count(Convertor.bytesToInt(Arrays.copyOfRange(buff, 22, 26)));
        sb.setTo_cl_bitmap(Convertor.bytesToInt(Arrays.copyOfRange(buff, 26, 30)));
        sb.setTo_inode_bitmap(Convertor.bytesToInt(Arrays.copyOfRange(buff, 30, 34)));
        sb.setTo_ilist(Convertor.bytesToInt(Arrays.copyOfRange(buff, 34, 38)));
        sb.setTo_root_cat(Convertor.bytesToInt(Arrays.copyOfRange(buff, 38, 42)));
        sb.setTo_data(Convertor.bytesToInt(Arrays.copyOfRange(buff, 42, 46)));
    }

    @FXML
    public void initialize() {
        form = new Formatter();
        go_to_formate.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                panel2.setVisible(false);
                panel1.setVisible(true);
            }
        });
        ok_button.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                try {
                    to_Next(file_choose.getValue().toString(), (Stage) ok_button.getScene().getWindow());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        File folder = new File(System.getProperty("user.dir"));
        for (File f : folder.listFiles()) {
            if (f.isFile() && f.getName().startsWith("OScore"))
                file_choose.getItems().add(f.getName());
        }
        if (file_choose.getItems().size() != 0)
            file_choose.setValue(file_choose.getItems().get(0));
        else {
            panel2.setVisible(false);
            panel1.setVisible(true);
        }
        formate_button.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                try {
                    short hd_size = Short.parseShort(disk_choose.getSelectionModel().getSelectedItem().toString());
                    short clust_size = Short.parseShort(clus_choose.getSelectionModel().getSelectedItem().toString());
                    sb = new Superblock();
                    sb.setCl_count(hd_size * 1024 * 1024 / clust_size);
                    if (hd_size == 80 && clust_size == 4096) {
                        ilist = new Inode[20000];
                        recs = new Record[1200];
                    } else {
                        ilist = new Inode[(int) Math.round(sb.getCl_count() * 0.9)];
                        recs = new Record[(int) Math.round(ilist.length * 0.06)];
                    }
                    inode_bitmap = new byte[ilist.length / 8];
                    for (int i = 0; i < inode_bitmap.length; i++)
                        inode_bitmap[i] = 0b0;
                    sb.setCl_size(clust_size);
                    sb.setInode_size(Inode.getSize());
                    sb.setInode_count(ilist.length);
                    sb.setFree_inodes_count(ilist.length);
                    if (Superblock.getSize() < clust_size)
                        sb.setTo_cl_bitmap(clust_size);
                    else
                        sb.setTo_cl_bitmap((int) Math.ceil(Superblock.getSize() / clust_size) * clust_size);
                    if (sb.getCl_count() / 8 <= clust_size)
                        sb.setTo_inode_bitmap(sb.getTo_cl_bitmap() + clust_size);
                    else
                        sb.setTo_inode_bitmap(sb.getTo_cl_bitmap() + (int) Math.ceil(sb.getCl_count() / clust_size / 8) * clust_size);
                    if (sb.getInode_count() / 8 <= clust_size)
                        sb.setTo_ilist(sb.getTo_inode_bitmap() + clust_size);
                    else
                        sb.setTo_ilist(sb.getTo_inode_bitmap() + (int) Math.ceil(ilist.length / clust_size / 8) * clust_size);
                    sb.setTo_root_cat(sb.getTo_ilist() + (int) Math.ceil(ilist.length * Inode.getSize() / clust_size) * clust_size);
                    sb.setTo_data(sb.getTo_root_cat() + (int) Math.ceil(recs.length * Record.getSize() / clust_size) * clust_size);
                    short taken = (short) Math.ceil(sb.getTo_data() / clust_size);
                    block_bitmap = new byte[sb.getCl_count() / 8];
                    for (int i = 0; i < block_bitmap.length; i++) {
                        block_bitmap[i] = (byte) 0b0;
                    }
                    blocks = new Block[sb.getCl_count() - taken];
                    System.out.println("Размер ЖД: " + hd_size + "\n" +
                            "Размер кластера: " + clust_size + "\n" +
                            "Число блоков: " + sb.getCl_count() + "\n" +
                            "Число inode-в: " + ilist.length + "\n" +
                            "Макс. число записей в корн. каталоге: " + recs.length + "\n" +
                            "Размер суперблока: " + Superblock.getSize() + "\n" + "" +
                            "Размер inode: " + Inode.getSize() + "\n" +
                            "Размер записи корн. каталога: " + Record.getSize() + "\n" +
                            "--------------------------------------------------------------------------------------------" +
                            "\nСмещения:\n" +
                            "1. К бит. карте блоков: " + sb.getTo_cl_bitmap() + "\n" +
                            "2. К бит. карте inode: " + sb.getTo_inode_bitmap() + "\n" +
                            "3. К массиву inode: " + sb.getTo_ilist() + "\n" +
                            "4. К корневому каталогу: " + sb.getTo_root_cat() + "\n" +
                            "5. К данным: " + sb.getTo_data());
                    /**Добавление корневого каталога*/
                    recs[0] = new Record();
                    recs[0].setFile_name("home");
                    recs[0].setName_length();
                    recs[0].setFile_ext("dir");
                    recs[0].setRec_length();
                    recs[0].setInode_num(0);
                    ilist[0] = new Inode();
                    ilist[0].setType_rights((short) 0b0111000111110110);
                    //Флаги (10-12 биты):
                    //001 - скрытый
                    //010 - системный
                    //100 - только чтение
                    //Типы файлов (13-15 биты):
                    //0 - обычный файл
                    //1 - корневой каталог
                    ilist[0].setUid((byte) 0);
                    ilist[0].setGid((byte) 0);
                    ilist[0].setCreate_date((new Date()).getTime());
                    ilist[0].setModif_date((new Date()).getTime());
                    ilist[0].setBlock_count((short) 1);
                    /**Добавление файла пользователей*/
                    recs[1] = new Record();
                    recs[1].setFile_name("users");
                    recs[1].setName_length();
                    recs[1].setFile_ext("txt");
                    recs[1].setRec_length();
                    recs[1].setInode_num(1);
                    ilist[1] = new Inode();
                    ilist[1].setType_rights((short) 0b0011000111100000);
                    ilist[1].setUid((byte) 0);
                    ilist[1].setGid((byte) 0);
                    ilist[1].setCreate_date((new Date()).getTime());
                    ilist[1].setModif_date((new Date()).getTime());
                    ilist[1].setBlock_count((short) 1);
                    ilist[1].setAddr(0, taken);
                    System.out.println("Users block: " + taken);
                    blocks[0] = new Block(clust_size);
                    String s = (char) 0 + "" + (char) 0 + "supreme_admin-" + MD5.hash("passadmin") + "\r\n" + (char) 1 + "" + (char) 1
                            + "SAPR-" + MD5.hash("PO") + "\r\n" + (char) 2 + "" + (char) 1 + "Student-" + MD5.hash("YaOtchislen") + "\r\n";
                    blocks[0].write(s.getBytes(), 0);
                    ilist[1].setFile_size(s.getBytes().length);
                    taken++;
                    /**Добавление файла групп*/
                    recs[2] = new Record();
                    recs[2].setFile_name("groups");
                    recs[2].setName_length();
                    recs[2].setFile_ext("txt");
                    recs[2].setRec_length();
                    recs[2].setInode_num(2);
                    ilist[2] = new Inode();
                    ilist[2].setType_rights((short) 0b0011000111100000);
                    ilist[2].setUid((byte) 0);
                    ilist[2].setGid((byte) 0);
                    ilist[2].setCreate_date((new Date()).getTime());
                    ilist[2].setModif_date((new Date()).getTime());
                    ilist[2].setBlock_count((short) 1);
                    ilist[2].setAddr(0, taken);
                    blocks[1] = new Block(clust_size);
                    System.out.println("Groups block: " + taken);
                    s = (char) 0 + "admins\r\n" + (char) 1 + "users\r\n";
                    blocks[1].write(s.getBytes(), 0);
                    ilist[2].setFile_size(s.getBytes().length);
                    //Типы групп:
                    //0 - администраторы
                    //1 - не администраторы
                    //taken++;
                    inode_bitmap[0] = (byte) 0b11100000;
                    int j = 0, i = 0, k = 0;
                    while (i <= taken) {
                        block_bitmap[j] |= 1 << (7 - k);
                        k++;
                        if (k == 8) {
                            j++;
                            k = 0;
                        }
                        i++;
                    }
                    for (i = 0; i < j; i++)
                        System.out.println(Integer.toBinaryString(block_bitmap[i]));
                    sb.setFree_blocks_count(sb.getCl_count() - taken);
                    sb.setFree_inodes_count(sb.getInode_count() - 3);
                    form.formate(hd_size, sb, block_bitmap, inode_bitmap, ilist, recs, blocks);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Ура!");
                    alert.setContentText("Форматирование успешно выполнено!");
                    alert.showAndWait();
                    to_Next("OScore (" + hd_size + ")", (Stage) formate_button.getScene().getWindow());
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Не ура...");
                    alert.setContentText(e.getMessage());
                    alert.show();
                }
            }
        });
        disk_choose.getItems().addAll("30", "50", "60", "80", "90", "100", "150", "200");
        disk_choose.setValue("80");
        clus_choose.getItems().addAll("512", "1024", "2048", "4096");
        clus_choose.setValue("4096");
    }
}
