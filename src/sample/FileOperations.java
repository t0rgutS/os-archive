package sample;

import OSComponents.*;
import Utilities.Convertor;
import Utilities.MD5;
import Utilities.UniExp;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;


enum search {inode, block, file}

enum fileop {write, del, attrs, sysattr, rights, show, rename}

public class FileOperations {
    private String file_name;
    private Superblock sb;
    private ArrayList<Record> files;
    private ArrayList<Inode> inodes;
    private byte UID;
    private byte GID;
    private byte maxUID, maxGID;
    private boolean choise;
    private volatile Planner planner;
    volatile Stage stage;

    public FileOperations(String file_name, Superblock sb) throws IOException, UniExp {
        this.file_name = file_name;
        this.sb = sb;
        formFiles();
        choise = false;
        planner = null;
    }

    public void close() {
        if (planner != null) {
            stage.close();
            planner.close();
        }
    }

    void start_planner() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("plannerform.fxml"));
        //loader.setController(planner);
        Parent root = loader.load();
        stage = new Stage();
        //Scene scene = new Scene(loader.load());
        stage.setScene(new Scene(root));
        stage.setTitle("Планировщик процессов");
        planner = loader.getController();
        planner.setBegin_users(new String[]{"supreme_admin", "SAPR", "Student"});
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        stage.setX(screen.getWidth() / 5 + 100);
        stage.hide();
        planner.start_fill();
        stage.setOnCloseRequest((WindowEvent event1) -> {
            stage.hide();
            planner.setFreezed(true);
            event1.consume();
        });
        Thread planner_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                planner.planning();
            }
        });
        planner_thread.start();
    }

    public void launch_process(boolean is_long) throws IOException {
        if (planner == null) {
            start_planner();
        }
        if (!stage.isShowing())
            stage.show();
        planner.update(is_long);
        planner.setFreezed(false);
    }

    public void create_proc(short CPU_burst) throws IOException, UniExp {
        if (planner == null) {
            start_planner();
        }
        if (files.get(1).getName_length() == -1)
            throw new UniExp("Отсутствует файл пользователей!");
        if (CPU_burst <= 0)
            throw new UniExp("CPU burst должно быть > 0!");
        if (CPU_burst < 10 || CPU_burst > 1500)
            throw new UniExp("CPU burst должно быть в пределах [10, 1500]!");
        String[] users = show_file(files.get(1).getFile_name() + "."
                + files.get(1).getFile_ext(), false).split("\r\n");
        int cur_user = 0;
        while ((byte) users[cur_user].charAt(0) != UID)
            cur_user++;
        byte queue_num;
        if (UID == 0)
            queue_num = 0;
        else if (GID == 0)
            queue_num = 1;
        else
            queue_num = 2;
        if (GID == 0)
            planner.set_new_process(users[cur_user].substring(2, users[cur_user].indexOf("-")), UID,
                    true, CPU_burst, queue_num);
        else
            planner.set_new_process(users[cur_user].substring(2, users[cur_user].indexOf("-")), UID,
                    false, CPU_burst, queue_num);
    }

    public void remove_proc(short PID, boolean instant) throws IOException, UniExp {
        if (planner == null) {
            start_planner();
        }
        if (PID < 0)
            throw new UniExp("Неверный формат!");
        if (GID == 0)
            planner.set_del_params(PID, UID, true, instant);
        else
            planner.set_del_params(PID, UID, false, instant);
    }

    public void change_ps_prio(short PID, byte correction) throws IOException, UniExp {
        if (planner == null) {
            start_planner();
        }
        if (PID < 0)
            throw new UniExp("Неверный формат!");
        if (GID == 0)
            planner.set_user_corr(PID, correction, true, UID);
        else
            planner.set_user_corr(PID, correction, false, UID);
    }

    public byte getUID() {
        return UID;
    }

    void formFiles() throws IOException, UniExp {
        files = new ArrayList<>();
        inodes = new ArrayList<>();
        RandomAccessFile raf = new RandomAccessFile(file_name, "r");
        int off;
        short length;
        byte[] len_buff = new byte[2];
        byte[] rec_buff;
        off = sb.getTo_root_cat();
        do {
            raf.seek(off);
            raf.read(len_buff);
            length = Convertor.bytesToShort(len_buff);
            if (length <= Record.getSize()) {
                rec_buff = new byte[length];
                rec_buff[0] = len_buff[0];
                rec_buff[1] = len_buff[1];
                raf.seek(off);
                raf.read(rec_buff);
                files.add(extract_Rec(rec_buff));
                off += length;
            }
        } while (length <= Record.getSize() && off < sb.getTo_data());
        byte[] inode_buff = new byte[sb.getInode_size()];
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).getName_length() != -1) {
                raf.seek(sb.getTo_ilist() + files.get(i).getInode_num() * sb.getInode_size());
                raf.read(inode_buff);
                inodes.add(extract_Inode(inode_buff));
                inodes.get(inodes.size() - 1).setNum(files.get(i).getInode_num());
            }
        }
        if (files.get(1).getName_length() == -1)
            throw new UniExp("Отсутствует файл пользователей!");
        if (files.get(2).getName_length() == -1)
            throw new UniExp("Отсутствует файл групп!");
        String[] temp = show_file(files.get(1).getFile_name() + "."
                + files.get(1).getFile_ext(), false).split("\r\n");
        maxUID = (byte) (temp[temp.length - 1].charAt(0));
        String[] gr_temp = show_file(files.get(2).getFile_name() + "."
                + files.get(2).getFile_ext(), false).split("\r\n");
        maxGID = (byte) (gr_temp[gr_temp.length - 1].charAt(0));
        UID = -1;
        GID = -1;
        raf.close();
    }

    public void sign_out() {
        UID = -1;
        GID = -1;
    }

    byte find_us_gr(String name, boolean user) throws IOException, UniExp {
        String[] temp;
        if (user) {
            temp = show_file(files.get(1).getFile_name() + "."
                    + files.get(1).getFile_ext(), false).split("\r\n");
            int i = 0;
            while (!name.equals(temp[i].substring(2, temp[i].indexOf("-")))) {
                i++;
                if (i == temp.length)
                    return -1;
            }
            return (byte) (temp[i].charAt(0));
        } else {
            temp = show_file(files.get(2).getFile_name() + "."
                    + files.get(2).getFile_ext(), false).split("\r\n");
            int i = 0;
            while (!name.equals(temp[i].substring(1))) {
                i++;
                if (i == temp.length)
                    return -1;
            }
            return (byte) (temp[i].charAt(0));
        }
    }

    public String change_user(String username, String pass) throws IOException, UniExp {
        if (files.get(1).getName_length() == -1)
            throw new UniExp("Отсутствует файл пользователей!");
        String[] temp = show_file(files.get(1).getFile_name() + "."
                + files.get(1).getFile_ext(), false).split("\r\n");
        int ind = 0;
        while (!username.equals(temp[ind].substring(2, temp[ind].indexOf("-")))) {
            ind++;
            if (ind == temp.length)
                throw new UniExp("Пользователя " + username + " не существует!");
        }
        if (MD5.hash(pass).equals(temp[ind].substring(temp[ind].indexOf("-") + 1))) {
            UID = (byte) (temp[ind].charAt(0));
            GID = (byte) (temp[ind].charAt(1));
            return "Теперь вы - " + username + "!";
        } else return "Неверный пароль!";
    }

    public void rename_us_gr(boolean user, String old_name, String new_name, String new_pass) throws IOException, UniExp {
        if (GID == 0) {
            new_name = new_name.replaceAll("-", "_").replaceAll(" ", "_")
                    .replaceAll("\t", "_").replaceAll("\n", "_")
                    .replaceAll("\r", "").replaceAll("_+", "_");
            if (new_name.getBytes().length > 100) {
                if (user)
                    throw new UniExp("Слишком длинное имя пользователя!");
                else
                    throw new UniExp("Слишком длинное название группы!");
            }
            if (new_pass.getBytes().length > 50)
                throw new UniExp("Слишком длинный пароль!");
            String[] temp;
            byte pos;
            if (user) {
                if (files.get(1).getName_length() == -1)
                    throw new UniExp("Отсутствует файл пользователей!");
                temp = show_file(files.get(1).getFile_name() + "."
                        + files.get(1).getFile_ext(), false).split("\r\n");
                pos = 0;
                while (!old_name.equals(temp[pos].substring(2, temp[pos].indexOf("-")))) {
                    pos++;
                    if (pos == temp.length)
                        throw new UniExp("Пользователя " + old_name + " не существует!");
                }
                if (find_us_gr(new_name, true) != -1)
                    throw new UniExp("Пользователь " + new_name + " уже зарегистрирован!");
                new_name = new_name.replaceAll("-", "");
                if (new_name.equals("") || new_name.equals(" "))
                    throw new UniExp("Некорректное имя пользователя!");
                temp[pos] = temp[pos].substring(0, 2) + new_name.replaceAll(" ", "_") + "-" + MD5.hash(new_pass);
                StringBuffer res = new StringBuffer();
                for (int i = 0; i < temp.length; i++)
                    res.append(temp[i] + "\r\n");
                write_file(files.get(1).getFile_name() + "."
                        + files.get(1).getFile_ext(), res.toString(), false);
            } else {
                if (files.get(2).getName_length() == -1)
                    throw new UniExp("Отсутствует файл групп!");
                temp = show_file(files.get(2).getFile_name() + "."
                        + files.get(2).getFile_ext(), false).split("\r\n");
                pos = 0;
                while (!old_name.equals(temp[pos].substring(1))) {
                    pos++;
                    if (pos == temp.length)
                        throw new UniExp("Группы " + old_name + " не существует!");
                }
                if (find_us_gr(new_name, false) != -1)
                    throw new UniExp("Группа " + new_name + " уже существует!");
                temp[pos] = temp[pos].charAt(0) + new_name.replaceAll(" ", "_");
                StringBuffer res = new StringBuffer();
                for (int i = 0; i < temp.length; i++)
                    res.append(temp[i] + "\r\n");
                write_file(files.get(2).getFile_name() + "."
                        + files.get(2).getFile_ext(), res.toString(), false);
            }
        }
    }

    public void change_pass(String username, String new_pass) throws IOException, UniExp {
        if (GID == 0) {
            if (files.get(1).getName_length() == -1)
                throw new UniExp("Отсутствует файл пользователей!");
            if (new_pass.getBytes().length > 50)
                throw new UniExp("Слишком длинный пароль!");
            int pos;
            if ((pos = find_us_gr(username, true)) != -1) {
                String[] users = show_file(files.get(1).getFile_name() + "."
                        + files.get(1).getFile_ext(), false).split("\r\n");
                users[pos] = users[pos].substring(0, 2) + username + "-" + MD5.hash(new_pass);
                StringBuffer buff = new StringBuffer();
                for (int i = 0; i < users.length; i++)
                    buff.append(users[i] + "\r\n");
                write_file(files.get(1).getFile_name() + "."
                        + files.get(1).getFile_ext(), buff.toString(), false);
            } else throw new UniExp("Пользователя " + username + " нет в системе!");
        } else throw new UniExp("Недостаточно прав!");
    }

    public void change_gr(String username, String newgroup) throws IOException, UniExp {
        if (GID == 0) {
            if (files.get(1).getName_length() == -1)
                throw new UniExp("Отсутствует файл пользователей!");
            if (files.get(2).getName_length() == -1)
                throw new UniExp("Отсутствует файл групп!");
            String[] temp = show_file(files.get(1).getFile_name() + "."
                    + files.get(1).getFile_ext(), false).split("\r\n");
            int ind = 0;
            while (!username.equals(temp[ind].substring(2, temp[ind].indexOf("-")))) {
                ind++;
                if (ind == temp.length)
                    throw new UniExp("Пользователя " + username + " нет в системе!");
            }
            byte group_ind = find_us_gr(newgroup, false);
            if (group_ind == -1)
                throw new UniExp("Группы " + newgroup + " не существует!");
            if (group_ind != (byte) (temp[ind].charAt(1))) {
                temp[ind] = temp[ind].charAt(0) + "" + (char) group_ind + "" + temp[ind].substring(2);
                StringBuffer res = new StringBuffer();
                for (int i = 0; i < temp.length; i++)
                    res.append(temp[i] + "\r\n");
                write_file(files.get(1).getFile_name() + "."
                        + files.get(1).getFile_ext(), res.toString(), false);
            }
        } else throw new UniExp("Недостаточно прав!");
    }

    public void create_us_gr(boolean user, String name, String pass, String gr) throws IOException, UniExp {
        if (GID == 0) {
            name = name.replaceAll("-", "_").replaceAll(" ", "_")
                    .replaceAll("\t", "_").replaceAll("\n", "_")
                    .replaceAll("\r", "").replaceAll("_+", "_");
            if (name.equals(""))
                throw new UniExp("Неверный формат!");
            if (!gr.equals(""))
                gr = gr.replaceAll("-", "_").replaceAll(" ", "_")
                        .replaceAll("\t", "_").replaceAll("\n", "_")
                        .replaceAll("\r", "").replaceAll("_+", "_");
            if (name.getBytes().length > 100) {
                if (user)
                    throw new UniExp("Слишком длинное имя пользователя!");
                else
                    throw new UniExp("Слишком длинное название группы!");
            }
            if (pass.getBytes().length > 50)
                throw new UniExp("Слишком длинный пароль!");
            if (user) {
                if (files.get(1).getName_length() == -1)
                    throw new UniExp("Отсутствует файл пользователей!");
                if (maxUID < Byte.MAX_VALUE) {
                    if (find_us_gr(name, true) == -1) {
                        byte group;
                        if ((group = find_us_gr(gr, false)) == -1)
                            throw new UniExp("Группы " + gr + " не существует!");
                        String new_user = (char) ((byte) (maxUID + 1)) + "" + (char) group + "" + name.replaceAll(" ", "_")
                                + "-" + MD5.hash(pass) + "\r\n";
                        Inode inode = inodes.get(search_inode(files.get(1).getInode_num()));
                        int file_size = inode.getFile_size() + new_user.getBytes().length;
                        if (file_size > (12 * sb.getCl_size() + sb.getCl_size() / 4
                                * sb.getCl_size()) || (sb.getFree_blocks_count() == 0 && file_size
                                > inode.getBlock_count() * sb.getCl_size()))
                            throw new UniExp("Не хватает места, чтобы добавить нового пользователя!");
                        write_file(files.get(1).getFile_name() + "."
                                + files.get(1).getFile_ext(), new_user, true);
                        maxUID++;
                    } else throw new UniExp("Пользователь " + name + " уже зарегестрирован!");
                } else throw new UniExp("Превышено максимальное число пользователей!");
            } else {
                if (files.get(2).getName_length() == -1)
                    throw new UniExp("Отсутствует файл групп!");
                if (maxGID < Byte.MAX_VALUE) {
                    if (find_us_gr(name, false) == -1) {
                        String new_group = (char) ((byte) (maxGID + 1)) + "" + name.replaceAll(" ", "_") + "\r\n";
                        Inode inode = inodes.get(search_inode(files.get(2).getInode_num()));
                        int file_size = inode.getFile_size() + new_group.getBytes().length;
                        if (file_size > (12 * sb.getCl_size() + sb.getCl_size() / 4
                                * sb.getCl_size()) || (sb.getFree_blocks_count() == 0 && file_size
                                > inode.getBlock_count() * sb.getCl_size()))
                            throw new UniExp("Не хватает места, чтобы добавить новую группу!");
                        write_file(files.get(2).getFile_name() + "."
                                + files.get(2).getFile_ext(), new_group, true);
                        maxGID++;
                    } else throw new UniExp("Группа " + name + " уже существует!");
                } else throw new UniExp("Превышено максимальное число групп!");
            }
        } else throw new UniExp("Недостаточно прав!");
    }

    boolean check_rights(Inode inode, fileop op) {
        BigInteger tr = BigInteger.valueOf(inode.getType_rights());
        if (tr.testBit(14))
            return false;
        if (op == fileop.show && (((UID == inode.getUid() && tr.testBit(8)) || (GID == inode.getGid() && tr.testBit(5))
                || (GID == 0 && inode.getGid() != 0) || (tr.testBit(2) && !tr.testBit(12)))))
            return true;
        if (op == fileop.write && !tr.testBit(11) && ((UID == inode.getUid() && tr.testBit(7))
                || (GID == inode.getGid() && tr.testBit(4)) || (GID == 0 && inode.getGid() != 0) || (tr.testBit(1) && !tr.testBit(12))))
            return true;
        if (op == fileop.rename && !tr.testBit(13) && ((UID == inode.getUid() && tr.testBit(7))
                || (GID == inode.getGid() && tr.testBit(4)) || (GID == 0 && inode.getGid() != 0) || (tr.testBit(1) && !tr.testBit(12))))
            return true;
        if (op == fileop.del && !tr.testBit(13) && ((UID == inode.getUid() && tr.testBit(6)) || (GID == inode.getGid() && tr.testBit(3))
                || (GID == 0 && inode.getGid() != 0) || (tr.testBit(0) && !tr.testBit(12))))
            return true;
        if ((op == fileop.attrs || op == fileop.rights) && (UID == inode.getUid() || (GID == 0 && inode.getGid() != 0)))
            return true;
        if (op == fileop.sysattr && GID == 0)
            return true;
        return false;
    }

    public boolean check_for_write(String name) throws IOException, UniExp {
        int pos;
        if ((pos = search_file(name)) != -1)
            return check_rights(inodes.get(search_inode(files.get(pos).getInode_num())), fileop.write);
        else throw new UniExp("Файла " + name + " не сущетвует!");
    }

    public void change_rights(String name, String newRights) throws IOException, UniExp {
        int pos;
        if ((pos = search_file(name)) != -1) {
            Record rec = files.get(pos);
            Inode inode = inodes.get(search_inode(rec.getInode_num()));
            short rights = inode.getType_rights();
            if (check_rights(inode, fileop.rights)) {
                if (Character.isDigit(newRights.charAt(0))) {
                    try {
                        int nr = Integer.parseInt(newRights);
                        if (nr > 777 || nr < 0)
                            throw new UniExp("Неверный формат команды!");
                        if (nr == 0)
                            throw new UniExp("Недопустимые права!");
                        String nr_binary = Integer.toBinaryString(nr / 100) + "";
                        while (nr_binary.length() < 3)
                            nr_binary += "0";
                        nr_binary += Integer.toBinaryString(nr / 10 % 10)
                                + "";
                        while (nr_binary.length() < 6)
                            nr_binary += "0";
                        nr_binary += Integer.toBinaryString(nr % 100 % 10);
                        while (nr_binary.length() < 9)
                            nr_binary += "0";
                        int k = 0;
                        for (int i = 8; i >= 0; i--) {
                            switch (nr_binary.charAt(k)) {
                                case '0':
                                    rights &= ~(1 << i);
                                    break;
                                case '1':
                                    rights |= 1 << i;
                                    break;
                            }
                            k++;
                        }
                    } catch (Exception e) {
                        throw new UniExp("Неверный формат команды!");
                    }
                } else if (newRights.startsWith("r") || newRights.startsWith("-")) {
                    if (newRights.length() < 9)
                        throw new UniExp("Неверный формат команды!");
                    int k = 0;
                    if (!(newRights.contains("r") || newRights.contains("w") || newRights.contains("x")))
                        throw new UniExp("Недопустимые права!");
                    for (int i = 8; i >= 0; i--) {
                        switch (newRights.charAt(k)) {
                            case 'r': {
                                if (i > 5)
                                    rights |= 1 << 8;
                                else if (i > 2)
                                    rights |= 1 << 5;
                                else
                                    rights |= 1 << 2;
                            }
                            break;
                            case 'w': {
                                if (i > 5)
                                    rights |= 1 << 7;
                                else if (i > 2)
                                    rights |= 1 << 4;
                                else
                                    rights |= 1 << 1;
                            }
                            break;
                            case 'x': {
                                if (i > 5)
                                    rights |= 1 << 6;
                                else if (i > 2)
                                    rights |= 1 << 3;
                                else
                                    rights |= 1 << 0;
                            }
                            break;
                            case '-':
                                rights &= ~(1 << i);
                                break;
                            default:
                                throw new UniExp("Неверный формат команды!");
                        }
                        k++;
                    }
                } else if ((newRights.startsWith("u") || newRights.startsWith("g")
                        || newRights.startsWith("o") || newRights.startsWith("a")) && (newRights.charAt(1) == '+'
                        || newRights.charAt(1) == '-')) {
                    if (newRights.length() < 3)
                        throw new UniExp("Неверный формат команды!");
                    BigInteger checker = BigInteger.valueOf(rights);
                    if ((newRights.equals("u-rwx") && !checker.testBit(0) && !checker.testBit(1) && !checker.testBit(2)
                            && !checker.testBit(3) && !checker.testBit(4) && !checker.testBit(5)) ||
                            (newRights.equals("g-rwx") && !checker.testBit(0) && !checker.testBit(1) && !checker.testBit(2)
                                    && !checker.testBit(6) && !checker.testBit(7) && !checker.testBit(8)) ||
                            (newRights.equals("o-rwx") && !checker.testBit(3) && !checker.testBit(4) && !checker.testBit(5)
                                    && !checker.testBit(6) && !checker.testBit(7) && !checker.testBit(8)) ||
                            newRights.equals("a-rwx"))
                        throw new UniExp("Недопустимые права!");
                    boolean append = true;
                    if (newRights.charAt(1) == '-')
                        append = false;
                    int adding = -1;
                    switch (newRights.charAt(0)) {
                        case 'u':
                            adding = 6;
                            break;
                        case 'g':
                            adding = 3;
                            break;
                        case 'o':
                            adding = 0;
                            break;
                    }
                    if (adding == -1) {
                        if (!newRights.substring(2, 5).contains("r") && !newRights.substring(2, 5).contains("w")
                                && !newRights.substring(2, 5).contains("x"))
                            throw new UniExp("Неверный формат команды!");
                        if (newRights.substring(2).contains("r")) {
                            if (append) {
                                rights |= 1 << 8;
                                rights |= 1 << 5;
                                rights |= 1 << 2;
                            } else {
                                rights &= ~(1 << 8);
                                rights &= ~(1 << 5);
                                rights &= ~(1 << 2);
                            }
                        }
                        if (newRights.substring(2, 5).contains("w")) {
                            if (append) {
                                rights |= 1 << 7;
                                rights |= 1 << 4;
                                rights |= 1 << 1;
                            } else {
                                rights &= ~(1 << 7);
                                rights &= ~(1 << 4);
                                rights &= ~(1 << 1);
                            }
                        }
                        if (newRights.substring(2, 5).contains("x")) {
                            if (append) {
                                rights |= 1 << 6;
                                rights |= 1 << 3;
                                rights |= 1 << 0;
                            } else {
                                rights &= ~(1 << 6);
                                rights &= ~(1 << 3);
                                rights &= ~(1 << 0);
                            }
                        }
                    } else {
                        if (!newRights.substring(2).contains("r") && !newRights.substring(2).contains("w")
                                && !newRights.substring(2).contains("x"))
                            throw new UniExp("Неверный формат команды!");
                        if (newRights.substring(2).contains("r")) {
                            if (append) {
                                rights |= 1 << (2 + adding);
                            } else {
                                rights &= ~(1 << (2 + adding));
                            }
                        }
                        if (newRights.substring(2).contains("w")) {
                            if (append) {
                                rights |= 1 << (1 + adding);
                            } else {
                                rights &= ~(1 << (1 + adding));
                            }
                        }
                        if (newRights.substring(2).contains("x")) {
                            if (append) {
                                rights |= 1 << adding;
                            } else {
                                rights &= ~(1 << adding);
                            }
                        }
                    }
                } else throw new UniExp("Неверный формат команды!");
                inode.setType_rights(rights);
                inode.setModif_date(new Date().getTime());
                writer(inode.toBytes(), sb.getTo_ilist() + rec.getInode_num() * sb.getInode_size());
            } else throw new UniExp("Отказано в доступе!");
        } else throw new UniExp("Файла " + name + " не существует!");
    }

    public void change_attrs(String name, String newAttrs) throws IOException, UniExp {
        int pos;
        if ((pos = search_file(name)) != -1) {
            Record rec = files.get(pos);
            Inode inode = inodes.get(search_inode(rec.getInode_num()));
            if (check_rights(inode, fileop.attrs)) {
                if (newAttrs.length() < 3) throw new UniExp("Неверный формат!");
                short res = inode.getType_rights();
                for (int i = 0; i < 3; i++) {
                    if (!Character.isDigit(newAttrs.charAt(i)))
                        throw new UniExp("Неверный формат!");
                    if ((i == 0 && check_rights(inode, fileop.sysattr)) || i != 0)
                        switch (newAttrs.charAt(i)) {
                            case '0':
                                res &= ~(1 << (13 - i));
                                break;
                            case '1':
                                res |= 1 << (13 - i);
                                break;
                            default:
                                throw new UniExp("Неверный формат!");
                        }
                }
                if (newAttrs.substring(0, 3).equals("100") && !check_rights(inode, fileop.sysattr))
                    throw new UniExp("Недостаточно прав, чтобы сделать файл системным!");
                inode.setType_rights(res);
                inode.setModif_date(new Date().getTime());
                writer(inode.toBytes(), sb.getTo_ilist() + rec.getInode_num() * sb.getInode_size());
            } else throw new UniExp("Отказано в доступе!");
        } else throw new UniExp("Файла " + name + " не существует!");
    }

    public void writer(byte[] arg, int position) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file_name, "rw");
        raf.seek(position);
        raf.write(arg);
        raf.close();
    }

    int firstFree(search what) throws IOException {
        byte[] blocks;
        RandomAccessFile raf = new RandomAccessFile(file_name, "r");
        int pos = 0, i = 0;
        if (what == search.inode) {
            blocks = new byte[sb.getInode_count()];
            raf.seek(sb.getTo_inode_bitmap());
        } else if (what == search.block) {
            blocks = new byte[sb.getCl_count()];
            raf.seek(sb.getTo_cl_bitmap());
        } else {
            blocks = new byte[Record.getSize()];
            raf.seek(sb.getTo_root_cat());
        }
        raf.read(blocks);
        raf.close();
        return pos;
    }

    String getUserAndGroup(Inode inode, boolean curruser) throws IOException, UniExp {
        StringBuffer found = new StringBuffer();
        String[] temp;
        if (files.get(1).getName_length() == -1)
            throw new UniExp("Отсутствует файл пользователей!");
        if (files.get(2).getName_length() == -1)
            throw new UniExp("Отсутствует файл групп!");
        temp = show_file(files.get(1).getFile_name() + "."
                + files.get(1).getFile_ext(), false).split("\r\n");
        int i = 0;
        if (inode != null) {
            while ((byte) (temp[i].charAt(0)) != inode.getUid())
                i++;
            found.append("Создатель: " + temp[i].substring(2, temp[i].indexOf("-")) + "\r\n");
            temp = show_file(files.get(2).getFile_name() + "."
                    + files.get(2).getFile_ext(), false).split("\r\n");
            i = 0;
            while ((byte) (temp[i].charAt(0)) != inode.getGid())
                i++;
            found.append("Группа: " + temp[i].substring(1) + "\r\n");
        } else if (curruser) {
            while ((byte) (temp[i].charAt(0)) != UID)
                i++;
            found.append("Текущий пользователь: " + temp[i].substring(2, temp[i].indexOf("-")));
        } else {
            String[] groups = show_file(files.get(2).getFile_name() + "."
                    + files.get(2).getFile_ext(), false).split("\r\n");
            int k;
            while (i < temp.length) {
                found.append("Пользователь: " + temp[i].substring(2, temp[i].indexOf("-")) + "\r\n");
                k = 0;
                while (groups[k].charAt(0) != temp[i].charAt(1))
                    k++;
                found.append("Группа: " + groups[k].substring(1) + "\r\n");
                i++;
                found.append("------------------------------\r\n");
            }
        }
        return found.toString();
    }

    public String getUsers(boolean current) throws IOException, UniExp {
        if (current)
            return getUserAndGroup(null, true);
        else {
            if (GID == 0)
                return getUserAndGroup(null, false);
            else throw new UniExp("Недостаточно прав!");
        }
    }

    public String getGroups() throws IOException, UniExp {
        if (GID == 0) {
            StringBuffer res = new StringBuffer();
            String[] temp;
            if (files.get(2).getName_length() == -1)
                throw new UniExp("Отсутствует файл групп!");
            temp = show_file(files.get(2).getFile_name() + "."
                    + files.get(2).getFile_ext(), false).split("\r\n");
            String[] users = new String[0];
            boolean us_exists = false;
            int us_count;
            if (files.get(1).getName_length() != -1) {
                users = show_file(files.get(1).getFile_name() + "."
                        + files.get(1).getFile_ext(), false).split("\r\n");
                us_exists = true;
            } else
                res.append("Не удалось подсчитать число пользователей в группах: отсутствует файл пользователей!\r\n");
            for (int i = 0; i < temp.length; i++) {
                res.append("Группа: " + temp[i].substring(1) + "\r\n");
                if (us_exists) {
                    us_count = 0;
                    for (int j = 0; j < users.length; j++) {
                        if (users[j].charAt(1) == temp[i].charAt(0))
                            us_count++;
                    }
                    res.append("Число пользователей: " + us_count + "\r\n");
                }
                res.append("----------------------------\r\n");
            }
            return res.toString();
        } else throw new UniExp("Недостаточно прав!");
    }

    String form_rights_attr(short typerights) {
        BigInteger big = BigInteger.valueOf(typerights);
        StringBuffer sb = new StringBuffer();
        sb.append("Атрибуты: ");
        if (big.testBit(13))
            sb.append("системный");
        if (big.testBit(12)) {
            if (sb.toString().equals("Атрибуты: "))
                sb.append("скрытый");
            else
                sb.append(", скрытый");
        }
        if (big.testBit(11)) {
            if (sb.toString().equals("Атрибуты: "))
                sb.append("только чтение");
            else
                sb.append(", только чтение");
        }
        if (sb.toString().equals("Атрибуты: "))
            sb.append("Обычный файл");
        sb.append("\r\nПрава доступа: ");
        int i = 8;
        while (i >= 0) {
            if (big.testBit(i)) {
                if (i == 8 || i == 5 || i == 2)
                    sb.append("r");
                else if (i == 7 || i == 4 || i == 1)
                    sb.append("w");
                else
                    sb.append("x");
            } else sb.append("-");
            i--;
        }
        sb.append("\r\n");
        return sb.toString();
    }

    public String show_files(boolean bucket) throws IOException, UniExp {
        StringBuffer res = new StringBuffer();
        int k;
        for (int i = 1; i < files.size(); i++) {
            if (files.get(i).getName_length() != -1) {
                k = search_inode(files.get(i).getInode_num());
                if ((bucket && files.get(i).getFile_name().charAt(0) == '$') || (!bucket
                        && files.get(i).getFile_name().charAt(0) != '$')) {
                    if (check_rights(inodes.get(k), fileop.show)) {
                        if (!bucket)
                            res.append("Имя: " + files.get(i).getFile_name() + "\r\n");
                        else
                            res.append("Имя: " + files.get(i).getFile_name().substring(1) + "\r\n");
                        String ext = files.get(i).getFile_ext();
                        if (!ext.equals("---"))
                            res.append("Расширение: " + ext + "\r\n");
                        res.append(getUserAndGroup(inodes.get(k), false) + form_rights_attr(inodes.get(k).getType_rights()) +
                                "Дата создания: " + new Date(inodes.get(k).getCreate_date()).toString() + "\r\nДата модификации: "
                                + new Date(inodes.get(k).getModif_date()).toString() + "\r\nРазмер: " + inodes.get(k).getFile_size()
                                + "\r\n------------------------------\r\n");
                    }
                }
            }
        }
        return res.toString();
    }

    public void create_file(String name) throws IOException, UniExp {
        if (sb.getFree_blocks_count() > 0 && sb.getFree_inodes_count() > 0) {
            StringBuffer fname = new StringBuffer();
            String fext;
            if (name.contains(".")) {
                fext = name.substring(name.indexOf(".") + 1).replaceAll(" ", "_")
                        .replaceAll("\t", "_").replaceAll("\n", "")
                        .replaceAll("\r", "");
                if (fext.getBytes().length < 3)
                    throw new UniExp("Неверный формат расширения!");
                if (fext.getBytes().length > 3) {
                    while (fext.getBytes().length > 3)
                        fext = fext.substring(0, fext.length() - 1);
                }
                if (fext.equals("dir"))
                    throw new UniExp("Недопустимое расширение файла!");
                fname.append(name.substring(0, name.indexOf(".")).replaceAll("$", "")
                        .replaceAll(" ", "_").replaceAll("\t", "_")
                        .replaceAll("\n", "_").replaceAll("\r", "")
                        .replaceAll("_+", "_"));
            } else {
                fext = "---";
                fname.append(name.replaceAll("$", "").replaceAll(" ", "_").replaceAll("\t", "_")
                        .replaceAll("\n", "_").replaceAll("\r", "")
                        .replaceAll("_+", "_"));
            }
            if (fname.toString().equals(" ") || fname.toString().equals(""))
                throw new UniExp("Неверное имя файла!");
            while (fname.toString().getBytes().length > 255) {
                fname.deleteCharAt(fname.length() - 1);
            }
            if (search_file(fname.toString() + "." + fext) != -1) {
                if (fname.toString().getBytes().length == 255)
                    fname.deleteCharAt(fname.length() - 1);
                int i = 1;
                while (search_file(fname.toString() + i) != -1) {
                    i++;
                    if (i % 10 == 0) {
                        if ((fname.toString() + i).getBytes().length == 255) {
                            fname.deleteCharAt(fname.length() - 1);
                            i = 1;
                        }
                    }
                }
                fname.append(i);
            }
            int file_pos = -1;
            int inode_pos = -1;
            int cl = firstFree(search.block);
            int inode = firstFree(search.inode);
            Record rec = new Record();
            rec.setFile_name(fname.toString());
            rec.setName_length();
            rec.setFile_ext(fext);
            rec.setInode_num(inode);
            rec.setRec_length();
            Inode node = new Inode();
            node.setUid(UID);
            node.setGid(GID);
            node.setBlock_count((short) 0);
            node.setCreate_date(new Date().getTime());
            node.setModif_date(new Date().getTime());
            node.setType_rights((short) 0b0000000111110100);
            node.setAddr(0, cl);
            node.setNum(inode);
            for (int i = 0; i < files.size(); i++) {
                if (files.get(i).getName_length() == -1 && files.get(i).getRec_length() == rec.getRec_length()) {
                    file_pos = i;
                    break;
                }
            }
            for (int i = 0; i < inodes.size(); i++) {
                if (inodes.get(i).getNum() == rec.getInode_num()) {
                    inode_pos = i;
                    break;
                }
            }
            sb.setFree_inodes_count(sb.getFree_inodes_count() - 1);
            sb.setFree_blocks_count(sb.getFree_blocks_count() - 1);
            if (file_pos != -1) {
                files.set(file_pos, rec);
            } else {
                if (has_place(rec.getRec_length())) {
                    files.add(rec);
                    file_pos = files.size() - 1;
                } else {
                    throw new UniExp("Недостаточно памяти. Попробуйте сначала очистить корзину.");
                }
            }
            if (inode_pos != -1) {
                inodes.set(inode_pos, node);
            } else {
                inodes.add(node);
                inode_pos = inodes.size() - 1;
            }
            writer(node.toBytes(), sb.getTo_ilist() + inode_pos * Inode.getSize());
            RandomAccessFile raf = new RandomAccessFile(file_name, "r");
            byte[] last = new byte[1];
            raf.seek(sb.getTo_inode_bitmap() + inode / 8);
            raf.read(last);
            last[0] |= 1 << (7 - inode % 8);
            writer(last, sb.getTo_inode_bitmap() + inode / 8);
            raf.seek(sb.getTo_cl_bitmap() + cl / 8);
            raf.read(last);
            last[0] |= 1 << (7 - cl % 8);
            writer(last, sb.getTo_cl_bitmap() + cl / 8);
            raf.close();
            int file_off = sb.getTo_root_cat();
            for (int i = 0; i < file_pos; i++)
                file_off += files.get(i).getRec_length();
            writer(rec.toBytes(), file_off);
            writer(sb.toBytes(), 0);
        } else {
            throw new UniExp("Недостаточно памяти. Попробуйте сначала очистить корзину.");
        }
    }

    public int search_file(String name) throws IOException {
        int pos = 0;
        String full_name;
        if (name.contains("."))
            full_name = name;
        else
            full_name = name + ".---";
        while (pos < files.size()) {
            if (files.get(pos).getName_length() != -1)
                if (full_name.equals(files.get(pos).getFile_name() + "." + files.get(pos).getFile_ext()))
                    return pos;
            pos++;
        }
        return -1;
    }

    public void remove_file(String name) throws IOException, UniExp {
        int pos;
        boolean move = true;
        if ((pos = search_file(name)) != -1) {
            if (check_rights(inodes.get(search_inode(files.get(pos).getInode_num())), fileop.del)) {
                Record rec = files.get(pos);
                if (rec.getName_length() == 255) {
                    String s = rec.getFile_name();
                    rec.setFile_name(s.substring(0, s.length() - 1));
                    move = false;
                }
                int off = sb.getTo_root_cat();
                for (int i = 0; i < pos; i++)
                    off += files.get(i).getRec_length();
                if (move) {
                    if (has_place(rec.getRec_length() + 1)) {
                        Record new_rec = new Record();
                        new_rec.setName_length((short) (rec.getName_length() + 1));
                        new_rec.setFile_name("$" + rec.getFile_name());
                        new_rec.setFile_ext(rec.getFile_ext());
                        new_rec.setInode_num(rec.getInode_num());
                        rec.setName_length((short) -1);
                        new_rec.setRec_length();
                        writer(files.get(pos).toBytes(), off);
                        int new_pos = -1;
                        for (int i = 0; i < files.size(); i++) {
                            if (files.get(i).getName_length() == -1
                                    && files.get(i).getRec_length() == new_rec.getRec_length()) {
                                new_pos = i;
                                break;
                            }
                        }
                        if (new_pos != -1) {
                            files.set(new_pos, new_rec);
                            for (int i = pos; i < new_pos - 1; i++)
                                off += files.get(i).getRec_length();
                        } else {
                            files.add(new_rec);
                            for (int i = pos; i < files.size() - 1; i++)
                                off += files.get(i).getRec_length();
                        }
                        writer(new_rec.toBytes(), off);
                    } else {
                        rec.setName_length((short) -1);
                        delete_file(null, rec);
                        writer(rec.toBytes(), off);
                        writer(sb.toBytes(), 0);
                        throw new UniExp("Файл " + name + " был удален без возможности восстановления из-за недостатка памяти!");
                    }
                } else {
                    rec.setFile_name("$" + rec.getFile_name());
                    writer(rec.toBytes(), off);
                }
            } else throw new UniExp("Недостаточно прав!");
        } else
            throw new UniExp("Файл " + name + " не существует!");
    }

    public void restore_file(String name) throws IOException, UniExp {
        int pos;
        if ((pos = search_file("$" + name)) != -1) {
            if (check_rights(inodes.get(search_inode(files.get(pos).getInode_num())), fileop.del)) {
                Record new_rec = new Record();
                new_rec.setFile_name(files.get(pos).getFile_name().substring(1));
                new_rec.setFile_ext(files.get(pos).getFile_ext());
                new_rec.setName_length();
                new_rec.setInode_num(files.get(pos).getInode_num());
                new_rec.setRec_length();
                int found = -1;
                for (int i = 1; i < files.size(); i++) {
                    if (files.get(i).getName_length() == -1 && files.get(i).getRec_length() == new_rec.getRec_length())
                        found = i;
                }
                int off = sb.getTo_root_cat();
                if (found == -1) {
                    if (has_place(new_rec.getRec_length())) {
                        files.add(new_rec);
                        for (int i = 0; i < files.size() - 1; i++)
                            off += files.get(i).getRec_length();
                        writer(new_rec.toBytes(), off);
                    } else throw new UniExp("Не удается восстановить файл: недостаточно места на диске!");
                } else {
                    files.set(found, new_rec);
                    for (int i = 0; i < found; i++)
                        off += files.get(i).getRec_length();
                    writer(new_rec.toBytes(), off);
                }
                files.get(pos).setName_length((short) -1);
                off = sb.getTo_root_cat();
                for (int i = 0; i < pos; i++)
                    off += files.get(i).getRec_length();
                writer(files.get(pos).toBytes(), off);
            } else throw new UniExp("Недостаточно прав!");
        } else throw new UniExp("Файла " + name + " нет в корзине!");
    }

    boolean has_place(int adding) throws IOException {
        int total = sb.getTo_data() - sb.getTo_root_cat();
        int all_files = 0;
        for (int i = 0; i < files.size(); i++) {
            if(files.get(i).getName_length() == -1 && files.get(i).getRec_length() == adding)
                return true;
            all_files += files.get(i).getRec_length();
        }
        all_files += adding;
        if (all_files < total)
            return true;
        else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Подождите!");
            alert.setContentText("Выполняется дефрагментация. Это может занять какое-то время...");
            alert.show();
            boolean deleted = false;
            int i = 0;
            while (i < files.size()) {
                if (files.get(i).getName_length() == -1) {
                    files.remove(i);
                    if (!deleted)
                        deleted = true;
                } else i++;
            }
            if (!deleted)
                return false;
            byte[] buff = new byte[total];
            for (int j = 0; j < total; j++)
                buff[j] = 46;
            writer(buff, sb.getTo_root_cat());
            int off = sb.getTo_root_cat();
            all_files = 0;
            for (int j = 0; j < files.size(); j++) {
                writer(files.get(j).toBytes(), off);
                off += files.get(j).getRec_length();
                all_files += files.get(j).getRec_length();
            }
            all_files += adding;
            if (all_files < total)
                return true;
            else
                return false;
        }
    }

    void delete_file(RandomAccessFile arg_raf, Record rec) throws IOException {
        RandomAccessFile raf;
        int addr;
        byte[] bl_buff = new byte[1];
        if (arg_raf == null)
            raf = new RandomAccessFile(file_name, "rw");
        else
            raf = arg_raf;
        int inode = search_inode(rec.getInode_num());
        for (int j = 0; j < 12; j++) {
            if ((addr = inodes.get(inode).getAddr(j)) != -1) {
                inodes.get(inode).setAddr(j, -1);
                raf.seek(sb.getTo_cl_bitmap() + addr / 8);
                raf.read(bl_buff);
                bl_buff[0] &= ~(1 << (7 - addr % 8));
                writer(bl_buff, sb.getTo_cl_bitmap() + addr / 8);
                sb.setFree_blocks_count(sb.getFree_blocks_count() + 1);
            }
        }
        if ((addr = inodes.get(inode).getIndir_addr()) != -1) {
            Block b = new Block(sb.getCl_size());
            raf.seek(addr * sb.getCl_size());
            raf.read(b.getData());
            int off = 0, curr, j = 12;
            while (j < inodes.get(inode).getBlock_count()) {
                curr = Convertor.bytesToInt(Arrays.copyOfRange(b.getData(), off, off + 4));
                raf.seek(sb.getTo_cl_bitmap() + curr / 8);
                raf.read(bl_buff);
                bl_buff[0] &= ~(1 << (7 - curr % 8));
                writer(bl_buff, sb.getTo_cl_bitmap() + curr / 8);
                sb.setFree_blocks_count(sb.getFree_blocks_count() + 1);
                j++;
            }
            raf.seek(sb.getTo_cl_bitmap() + addr / 8);
            raf.read(bl_buff);
            bl_buff[0] &= ~(1 << (7 - addr % 8));
            writer(bl_buff, sb.getTo_cl_bitmap() + addr / 8);
            sb.setFree_blocks_count(sb.getFree_blocks_count() + 1);
            b.clear();
            raf.seek(addr * sb.getCl_size());
            raf.write(b.getData());
            inodes.get(inode).setIndir_addr(-1);

        }
        raf.seek(sb.getTo_inode_bitmap() + rec.getInode_num() / 8);
        raf.read(bl_buff);
        bl_buff[0] &= ~(1 << (7 - rec.getInode_num() % 8));
        writer(bl_buff, sb.getTo_inode_bitmap() + rec.getInode_num() / 8);
        sb.setFree_inodes_count(sb.getFree_inodes_count() + 1);
        if (arg_raf == null)
            raf.close();
    }

    public void clear_bucket() throws IOException {
        boolean deleted = false;
        RandomAccessFile raf = new RandomAccessFile(file_name, "rw");
        for (int i = 1; i < files.size(); i++) {
            if (files.get(i).getName_length() != -1) {
                if (files.get(i).getFile_name().charAt(0) == '$' &&
                        check_rights(inodes.get(search_inode(files.get(i).getInode_num())), fileop.del)) {
                    files.get(i).setName_length((short) -1);
                    if (!deleted)
                        deleted = true;
                    delete_file(raf, files.get(i));
                }
            }
        }
        if (deleted) {
            int off = sb.getTo_root_cat();
            for (int i = 0; i < files.size(); i++) {
                if (files.get(i).getName_length() == -1)
                    writer(files.get(i).toBytes(), off);
                off += files.get(i).getRec_length();
            }
            writer(sb.toBytes(), 0);
        }
        raf.close();
    }

    public String show_file(String name, boolean checkRights) throws IOException, UniExp {
        int pos;
        StringBuffer res = new StringBuffer();
        if ((pos = search_file(name)) != -1) {
            RandomAccessFile raf = new RandomAccessFile(file_name, "r");
            Record rec = files.get(pos);
            Inode inode = inodes.get(search_inode(rec.getInode_num()));
            if (checkRights && !check_rights(inode, fileop.show))
                throw new UniExp("Недостаточно прав!");
            Block b = new Block(sb.getCl_size());
            int sum = 0, i = 0, current_block = 0;
            byte[] indir = new byte[1];
            while (sum < inode.getFile_size()) {
                if (i < 12) {
                    current_block = inode.getAddr(i);
                } else {
                    if (indir.length == 1) {
                        indir = new byte[sb.getCl_size()];
                        raf.seek(inode.getIndir_addr() * sb.getCl_size());
                        raf.read(indir);
                    }
                    current_block = Convertor.bytesToInt(Arrays.copyOfRange(indir, (i - 12) * 4, (i - 12) * 4 + 4));
                }
                raf.seek(current_block * sb.getCl_size());
                raf.read(b.getData());
                if (sum + sb.getCl_size() < inode.getFile_size()) {
                    res.append(new String(b.getData()));
                    sum += sb.getCl_size();
                } else {
                    res.append(new String(b.getSome(inode.getFile_size() - sum)));
                    sum = inode.getFile_size();
                }
                i++;
            }
            raf.close();
            return res.toString();
        } else throw new UniExp("Файла " + name + " не существует!");
    }

    Inode extract_Inode(byte[] arg) {
        Inode inode = new Inode();
        inode.setFile_size(Convertor.bytesToInt(Arrays.copyOfRange(arg, 0, 4)));
        inode.setType_rights(Convertor.bytesToShort(Arrays.copyOfRange(arg, 4, 6)));
        inode.setUid(arg[6]);
        inode.setGid(arg[7]);
        inode.setCreate_date(Convertor.bytesToLong(Arrays.copyOfRange(arg, 8, 16)));
        inode.setModif_date(Convertor.bytesToLong(Arrays.copyOfRange(arg, 16, 24)));
        inode.setBlock_count(Convertor.bytesToShort(Arrays.copyOfRange(arg, 24, 26)));
        for (int i = 0; i < 12; i++)
            inode.setAddr(i, Convertor.bytesToInt(Arrays.copyOfRange(arg, 26 + i * 4, 26 + (i + 1) * 4)));
        inode.setIndir_addr(Convertor.bytesToInt(Arrays.copyOfRange(arg, 74, 78)));
        return inode;
    }

    Record extract_Rec(byte[] arg) {
        Record rec = new Record();
        rec.setRec_length(Convertor.bytesToShort(Arrays.copyOfRange(arg, 0, 2)));
        rec.setName_length(Convertor.bytesToShort(Arrays.copyOfRange(arg, 2, 4)));
        if (rec.getName_length() != -1) {
            rec.setFile_name(new String(Arrays.copyOfRange(arg, 4, rec.getName_length() + 4)));
            rec.setFile_ext(new String(Arrays.copyOfRange(arg, 4 + rec.getName_length(), rec.getName_length() + 7)));
            rec.setInode_num(Convertor.bytesToInt(Arrays.copyOfRange(arg, rec.getName_length() + 7, rec.getName_length() + 11)));
        }
        return rec;
    }

    int search_inode(int num) {
        int pos = 0;
        while (inodes.get(pos).getNum() != num) {
            pos++;
            if (pos == inodes.size())
                return -1;
        }
        return pos;
    }

    public void write_file(String name, String arg, boolean append) throws IOException, UniExp {
        int pos;
        if ((pos = search_file(name)) == -1) {
            if (append)
                throw new UniExp("Файла " + name + " не существует!");
            else {
                create_file(name);
                pos = search_file(name);
            }
        }
        RandomAccessFile raf = new RandomAccessFile(file_name, "r");
        byte[] last_b_or_i = new byte[1];
        byte[] byted_arg = arg.getBytes();
        Record rec = files.get(pos);
        Inode inode = inodes.get(search_inode(rec.getInode_num()));
        short new_block_count = inode.getBlock_count();
        Block b = new Block(sb.getCl_size());
        boolean block_add = false;
        int off = 0, free = 0, free_bytes = 0;
        if (append) {
            if (inode.getFile_size() % sb.getCl_size() != 0) {
                if (inode.getBlock_count() <= 12) {
                    int i = 0;
                    while (inode.getAddr(i) != -1 && i < 12)
                        i++;
                    if (i < 12)
                        i--;
                    raf.seek(inode.getAddr(i) * sb.getCl_size());
                    raf.read(b.getData());
                    free_bytes = sb.getCl_size() - inode.getFile_size() % sb.getCl_size();
                    if (off + free_bytes < byted_arg.length) {
                        b.write(Arrays.copyOfRange(byted_arg, off, off + free_bytes),
                                inode.getFile_size() % sb.getCl_size());
                        off += free_bytes;
                    } else {
                        b.write(Arrays.copyOfRange(byted_arg, off, byted_arg.length), inode.getFile_size()
                                % sb.getCl_size());
                        off = byted_arg.length;
                    }
                    writer(b.getData(), inode.getAddr(i) * sb.getCl_size());
                } else {
                    raf.seek(inode.getIndir_addr() * sb.getCl_size());
                    raf.read(b.getData());
                    int last_offset, last;
                    if (inode.getBlock_count() == 12 + sb.getCl_size() / 4)
                        last_offset = sb.getCl_size() - 4;
                    else
                        last_offset = (inode.getBlock_count() - 12) * 4 - 4;
                    last = Convertor.bytesToInt(Arrays.copyOfRange(b.getData(), last_offset, last_offset + 4));
                    raf.seek(last * sb.getCl_size());
                    raf.read(b.getData());
                    free_bytes = sb.getCl_size() - inode.getFile_size() % sb.getCl_size();
                    if (off + free_bytes < arg.getBytes().length) {
                        b.write(Arrays.copyOfRange(byted_arg, off, off + free_bytes), inode.getFile_size()
                                % sb.getCl_size());
                        off += free_bytes;
                    } else {
                        b.write(Arrays.copyOfRange(byted_arg, off, byted_arg.length), inode.getFile_size()
                                % sb.getCl_size());
                        off = byted_arg.length;
                    }
                    writer(b.getData(), last * sb.getCl_size());
                }
            }
        }
        try {
            if (off < byted_arg.length) {
                short i = 0;
                int block_off = 0;
                if (!append)
                    new_block_count = 0;
                while (off < byted_arg.length) {
                    if (i < 12) {
                        if (inode.getAddr(i) == -1) {
                            free = firstFree(search.block);
                            inode.setAddr(i, free);
                            writer(inode.toBytes(), sb.getTo_ilist() + rec.getInode_num() * Inode.getSize());
                            raf.seek(sb.getTo_cl_bitmap() + free / 8);
                            raf.read(last_b_or_i);
                            last_b_or_i[0] |= 1 << (7 - free % 8);
                            writer(last_b_or_i, sb.getTo_cl_bitmap() + free / 8);
                            new_block_count++;
                            if (!block_add)
                                block_add = true;
                            sb.setFree_blocks_count(sb.getFree_blocks_count() - 1);
                        } else if (!append)
                            new_block_count++;
                        block_off = inode.getAddr(i);
                    } else {
                        if (inode.getIndir_addr() == -1) {
                            free = firstFree(search.block);
                            inode.setIndir_addr(free);
                            raf.seek(sb.getTo_cl_bitmap() + free / 8);
                            raf.read(last_b_or_i);
                            last_b_or_i[0] |= 1 << (7 - free % 8);
                            writer(last_b_or_i, sb.getTo_cl_bitmap() + free / 8);
                            if (!block_add)
                                block_add = true;
                            sb.setFree_blocks_count(sb.getFree_blocks_count() - 1);
                            b.clear();
                            free = firstFree(search.block);
                            b.write(Convertor.intToBytes(free), 0);
                            writer(b.getData(), inode.getIndir_addr() * sb.getCl_size());
                        }
                        if (new_block_count == 12 + sb.getCl_size() / 4)
                            throw new UniExp("Запись остановлена: превышен макс. размер файла!");
                        b.clear();
                        raf.seek(inode.getIndir_addr() * sb.getCl_size());
                        raf.read(b.getData());
                        if (new_block_count > inode.getBlock_count()) {
                            free = firstFree(search.block);
                            b.write(Convertor.intToBytes(free), (i - 12) * 4);
                            writer(b.getData(), inode.getIndir_addr() * sb.getCl_size());
                            raf.seek(sb.getTo_cl_bitmap() + free / 8);
                            raf.read(last_b_or_i);
                            last_b_or_i[0] |= 1 << (7 - free % 8);
                            writer(last_b_or_i, sb.getTo_cl_bitmap() + free / 8);
                            new_block_count++;
                            block_add = true;
                            sb.setFree_blocks_count(sb.getFree_blocks_count() - 1);
                        } else {
                            free = Convertor.bytesToInt(Arrays.copyOfRange(b.getData(), (i - 12) * 4, (i - 12) * 4 + 4));
                            if (!append)
                                new_block_count++;
                        }
                        block_off = free;
                    }
                    b.clear();
                    if (off + sb.getCl_size() < arg.getBytes().length) {
                        b.write(Arrays.copyOfRange(byted_arg, off, off + sb.getCl_size()), 0);
                        off += sb.getCl_size();
                    } else {
                        b.write(Arrays.copyOfRange(byted_arg, off, byted_arg.length), 0);
                        off = byted_arg.length;
                    }
                    writer(b.getData(), block_off * sb.getCl_size());
                    i++;
                }
            }
        } catch (UniExp ue) {
            throw new UniExp(ue.getMessage());
        } finally {
            if (!append && inode.getBlock_count() > new_block_count) {
                byte[] filler = {46, 46, 46, 46};
                byte[] from_bitmap = new byte[1];
                int curr = 0;
                if (inode.getBlock_count() > 12) {
                    b.clear();
                    raf.seek(inode.getIndir_addr() * sb.getCl_size());
                    raf.read(b.getData());
                }
                for (short i = new_block_count; i < inode.getBlock_count(); i++) {
                    if (i < 12) {
                        curr = inode.getAddr(i);
                        inode.setAddr(i, -1);
                    } else {
                        curr = Convertor.bytesToInt(Arrays.copyOfRange(b.getData(), (i - 12) * 4,
                                (i - 12) * 4 + 4));
                        b.write(filler, (i - 12) * 4);
                    }
                    raf.seek(sb.getTo_cl_bitmap() + curr / 8);
                    raf.read(from_bitmap);
                    from_bitmap[0] &= ~(1 << (7 - curr % 8));
                    sb.setFree_blocks_count(sb.getFree_blocks_count() + 1);
                    writer(from_bitmap, sb.getTo_cl_bitmap() + curr / 8);
                }
                if (inode.getIndir_addr() != -1)
                    writer(b.getData(), inode.getIndir_addr() * sb.getCl_size());
                if (new_block_count < 12 && inode.getIndir_addr() != -1) {
                    raf.seek(sb.getTo_cl_bitmap() + inode.getIndir_addr() / 8);
                    raf.read(from_bitmap);
                    from_bitmap[0] &= ~(1 << (7 - inode.getIndir_addr() % 8));
                    writer(from_bitmap, sb.getTo_cl_bitmap() + inode.getIndir_addr() / 8);
                    inode.setIndir_addr(-1);
                    sb.setFree_blocks_count(sb.getFree_blocks_count() + 1);
                }
                writer(sb.toBytes(), 0);
            } else if (block_add)
                writer(sb.toBytes(), 0);
            inode.setBlock_count(new_block_count);
            inode.setModif_date(new Date().getTime());
            if (append)
                inode.setFile_size(inode.getFile_size() + off);
            else
                inode.setFile_size(off);
            System.out.println("Write block count: " + inode.getBlock_count());
            writer(inode.toBytes(), sb.getTo_ilist() + rec.getInode_num() * Inode.getSize());
            raf.close();
        }
    }

    public void rename_file(String old_name, String new_name) throws IOException, UniExp {
        int pos, pos2 = -1;
        if ((pos = search_file(old_name)) != -1) {
            if (check_rights(inodes.get(search_inode(files.get(pos).getInode_num())), fileop.rename)) {
                Record rec = files.get(pos);
                if (new_name.contains(".")) {
                    String ext = new_name.substring(new_name.indexOf(".")).replaceAll(" ", "_").replaceAll("\t", "_")
                            .replaceAll("\n", "").replaceAll("\r", "");
                    if (ext.getBytes().length < 3)
                        throw new UniExp("Неверный формат расширения!");
                    else {
                        while (ext.getBytes().length > 3)
                            ext = ext.substring(0, ext.length() - 1);
                    }
                    rec.setFile_ext(ext);
                    new_name = new_name.substring(0, new_name.indexOf(".")).replaceAll("$", "")
                            .replaceAll(" ", "_").replaceAll("\t", "_")
                            .replaceAll("\n", "_").replaceAll("\r", "")
                            .replaceAll("_+", "_");
                } else {
                    rec.setFile_ext("---");
                    new_name = new_name.replaceAll("$", "").replaceAll(" ", "_")
                            .replaceAll("\t", "_")
                            .replaceAll("\n", "_").replaceAll("\r", "")
                            .replaceAll("_+", "_");
                }
                if (new_name.equals(" ") || new_name.equals(""))
                    throw new UniExp("Неверное имени файла!");
                if (new_name.length() > 255)
                    new_name = new_name.substring(0, 255);
                while (new_name.getBytes().length > 255)
                    new_name = new_name.substring(0, new_name.length() - 1);
                if (new_name.getBytes().length == rec.getName_length())
                    rec.setFile_name(new_name);
                else {
                    files.get(pos).setName_length((short) -1);
                    Record new_rec = new Record();
                    new_rec.setFile_name(new_name);
                    new_rec.setFile_ext(rec.getFile_ext());
                    new_rec.setName_length();
                    new_rec.setInode_num(rec.getInode_num());
                    new_rec.setRec_length();
                    for (int i = 0; i < files.size(); i++) {
                        if (files.get(i).getName_length() == -1 && files.get(i).getRec_length() == new_rec.getRec_length()) {
                            pos2 = i;
                            break;
                        }
                    }
                    if (pos2 == -1) {
                        if (has_place(new_rec.getRec_length())) {
                            files.add(new_rec);
                            pos2 = files.size() - 1;
                        } else throw new UniExp("Недостаточно места! Возможно, стоит очистить корзину.");
                    } else
                        files.set(pos2, new_rec);
                }
                int off = sb.getTo_root_cat();
                for (int i = 0; i < pos; i++)
                    off += files.get(i).getRec_length();
                writer(files.get(pos).toBytes(), off);
                if (pos2 != -1) {
                    off = 0;
                    for (int i = 0; i < pos2; i++)
                        off += files.get(i).getRec_length();
                    writer(files.get(pos2).toBytes(), off);
                }
            } else throw new UniExp("Недостаточно прав!");
        } else throw new UniExp("Файла " + old_name + " не существует!");
    }

    public void del_us_gr(String name, boolean group) throws IOException, UniExp {
        if (GID == 0) {
            if (group) {
                if (files.get(2).getName_length() == -1)
                    throw new UniExp("Отсутствует файл пользователей!");
            } else {
                if (files.get(1).getName_length() == -1)
                    throw new UniExp("Отсутствует файл пользователей!");
            }
            String temp[];
            if (group)
                temp = show_file(files.get(2).getFile_name() + "."
                        + files.get(2).getFile_ext(), false).split("\r\n");
            else
                temp = show_file(files.get(1).getFile_name() + "."
                        + files.get(1).getFile_ext(), false).split("\r\n");
            byte id = -1;
            int delete_ind = 0;
            for (int i = 0; i < temp.length; i++) {
                if ((group && temp[i].substring(1).equals(name)) ||
                        (!group && temp[i].substring(2, temp[i].indexOf("-")).equals(name))) {
                    delete_ind = i;
                    id = (byte) (temp[i].charAt(0));
                    break;
                }
            }
            if (id == -1) {
                if (group)
                    throw new UniExp("Группы " + name + " не существует!");
                else
                    throw new UniExp("Пользователя " + name + " не существует!");
            }
            if (group) {
                if (id == 0)
                    throw new UniExp("Нельзя удалить группу администраторов!");
                if (files.get(1).getName_length() == -1)
                    throw new UniExp("Отсутствует файл пользователей!");
                String[] users = show_file(files.get(1).getFile_name() + "."
                        + files.get(1).getFile_ext(), false).split("\r\n");
                for (int i = 0; i < users.length; i++) {
                    if ((byte) (users[i].charAt(1)) == id)
                        del_us_gr(users[i].substring(2, users[i].indexOf("-")), false);
                }
            } else {
                if (id == UID)
                    throw new UniExp("Нельзя удалить текущего пользователя!");
                if (id == 0)
                    throw new UniExp("Нельзя удалить главного администратора!");
                for (int i = 0; i < files.size(); i++) {
                    Inode inode = inodes.get(search_inode(files.get(i).getInode_num()));
                    if (inode.getUid() == id && check_rights(inode, fileop.del)) {
                        delete_file(null, files.get(i));
                        files.get(i).setName_length((short) -1);
                        int off = sb.getTo_root_cat();
                        for (int j = 0; j < i; j++)
                            off += files.get(j).getRec_length();
                        writer(files.get(i).toBytes(), off);
                    }
                }
            }
            StringBuffer buff = new StringBuffer();
            for (int i = 0; i < temp.length; i++) {
                if (i != delete_ind)
                    buff.append(temp[i] + "\r\n");
            }
            if (group) {
                maxGID--;
                write_file(files.get(2).getFile_name() + "."
                        + files.get(2).getFile_ext(), buff.toString(), false);
            } else {
                maxUID--;
                write_file(files.get(1).getFile_name() + "."
                        + files.get(1).getFile_ext(), buff.toString(), false);
            }
        } else throw new UniExp("Недостаточно прав!");
    }

    public void copy_file(String original, String copy_to) throws IOException, UniExp {
        int pos;
        if ((pos = search_file(original)) != -1) {
            if (original.equals(copy_to))
                throw new UniExp("Нельзя скопировать файл сам в себя!");
            if (check_rights(inodes.get(search_inode(files.get(pos).getInode_num())), fileop.show)) {
                int pos2 = search_file(copy_to);
                if (pos2 != -1 && !check_rights(inodes.get(search_inode(files.get(pos2).getInode_num())), fileop.write))
                    throw new UniExp("Недостаточно прав!");
                write_file(copy_to, show_file(files.get(pos).getFile_name(), false), false);
            } else throw new UniExp("Недостаточно прав!");
        } else throw new UniExp("Файла " + original + " не существует!");
    }
}
