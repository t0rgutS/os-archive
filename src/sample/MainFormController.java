package sample;

import OSComponents.Superblock;
import Utilities.UniExp;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

enum curr {normal, write, append}

public class MainFormController {
    @FXML
    TextArea area;
    private String file_name;
    private String[] commands = {"create", "show", "write", "change", "delete", "clear", "bucket", "crus", "crgr",
            "sign", "chgr", "rename", "attrs", "rights", "restore", "curuser", "users", "groups", "delus", "delgr",
            "rnus", "rngr", "chpass", "copy", "exit", "system off", "ps", "new", "kill", "prio"};
    private FileOperations oper;
    private StringBuffer bak, chosen_file;
    private ArrayList<String> last_commands;
    private int last_indic;
    private curr current;
    private byte UID, GID;
    private Superblock sb;

    public void setFile_name(String file_name, Superblock sb) {
        this.file_name = file_name;
        this.sb = sb;
        System.out.println("block bitmap: " + sb.getTo_cl_bitmap());
        System.out.println("inode bitmap: " + sb.getTo_inode_bitmap());
        System.out.println("ilist: " + sb.getTo_ilist());
        System.out.println("root catalog: " + sb.getTo_root_cat());
        System.out.println("data: " + sb.getTo_data());
    }

    int normIndexOf(String arg, char find, int order) {
        int found = 0;
        for (int i = 0; i < arg.length(); i++) {
            if (arg.charAt(i) == find) {
                found++;
                if (found == order)
                    return i;
            }
        }
        return -1;
    }

    @FXML
    public void initialize() {
        current = curr.normal;
        bak = new StringBuffer();
        chosen_file = new StringBuffer();
        last_commands = new ArrayList<>();
        last_indic = -1;
        UID = 0;
        GID = 0;
        area.setText("/home/$: ");
        area.positionCaret(area.getText().length());
        area.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                try {
                    if (oper == null) {
                        oper = new FileOperations(file_name, sb);
                        area.getScene().getWindow().setOnCloseRequest(new EventHandler<WindowEvent>() {
                            @Override
                            public void handle(WindowEvent event) {
                                if (oper != null)
                                    oper.close();
                            }
                        });
                    }
                    if (current == curr.normal) {
                        if (area.getCaretPosition() <= area.getText().lastIndexOf("/home/$: ") + 9)
                            area.positionCaret(area.getText().length());
                        if (event.getCode() == KeyCode.BACK_SPACE) {
                            if (area.getCaretPosition() <= area.getText().lastIndexOf("/home/$: ") + 9)
                                event.consume();
                        } else if (event.getCode() == KeyCode.DELETE) {
                            if (area.getCaretPosition() < area.getText().lastIndexOf("/home/$: ") + 9)
                                event.consume();
                        }
                        if (event.getCode() == KeyCode.ENTER) {
                            try {
                                String line;
                                line = area.getText().substring(area.getText().lastIndexOf("/home/$: ") + 9).trim();
                                if (line.equals("") || line.equals(" "))
                                    throw new UniExp("Неизвестная команда!");
                                last_commands.add(line);
                                if (last_commands.size() > 5)
                                    last_commands.remove(0);
                                last_indic = last_commands.size() - 1;
                                int i = 0;
                                while (!line.equals(commands[i]) && !line.startsWith(commands[i] + " ")) {
                                    i++;
                                    if (i == commands.length) throw new UniExp("Неизвестная команда!");
                                }
                                if (oper.getUID() == -1 && i != 9)
                                    throw new UniExp("Авторизируйтесь!");
                                if (oper.getUID() != -1 && i == 9)
                                    throw new UniExp("Сначала выйдите из системы!");
                                switch (i) {
                                    case 0: {
                                        String name;
                                        try {
                                            name = line.substring(commands[i].length() + 1);
                                            if (name.equals("") || name.equals(" "))
                                                throw new UniExp("Имя файла не должно быть пустым!");
                                        } catch (IndexOutOfBoundsException e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        oper.create_file(name);
                                        area.setText(area.getText() + "\r\nФайл добавлен!\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 1: {
                                        String s;
                                        if (!(line.equals("show -a") || line.startsWith("show -a "))) {
                                            try {
                                                s = oper.show_file(line.substring(commands[i].length() + 1), true);
                                            } catch (IndexOutOfBoundsException e) {
                                                throw new UniExp("Неверный формат!");
                                            }
                                        } else
                                            s = oper.show_files(false);
                                        area.setText(area.getText() + "\r\n" + s + "\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 2: {
                                        try {
                                            if (line.substring(commands[i].length() + 1, commands[i].length() + 3).equals("> ")) {
                                                current = curr.write;
                                                chosen_file.append(line.substring(commands[i].length() + 3));
                                            } else {
                                                current = curr.append;
                                                chosen_file.append(line.substring(commands[i].length() + 4));
                                            }
                                            boolean checker;
                                            try {
                                                checker = oper.check_for_write(chosen_file.toString());
                                            } catch (UniExp ue) {
                                                if (current == curr.write)
                                                    checker = true;
                                                else
                                                    checker = false;
                                            }
                                            if (checker) {
                                                bak.append(area.getText().trim());
                                                area.setText("");
                                            } else {
                                                area.setText(area.getText() + "\r\nНедостаточно прав или файла "
                                                        + chosen_file.toString() + " не существует!\r\n/home/$: ");
                                                area.positionCaret(area.getText().length());
                                                chosen_file.delete(0, chosen_file.length());
                                                current = curr.normal;

                                            }
                                        } catch (IndexOutOfBoundsException e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                    }
                                    break;
                                    case 3: {
                                        try {
                                            chosen_file.append(line.substring(commands[i].length() + 1));
                                            if (oper.check_for_write(chosen_file.toString())) {
                                                bak.append(area.getText().trim());
                                                area.setText(oper.show_file(chosen_file.toString(), true));
                                                current = curr.write;
                                            } else {
                                                area.setText(area.getText() + "\r\nНедостаточно прав!\r\n/home/$: ");
                                                area.positionCaret(area.getText().length());
                                            }
                                        } catch (IndexOutOfBoundsException e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                    }
                                    break;
                                    case 4: {
                                        try {
                                            oper.remove_file(line.substring(commands[i].length() + 1));
                                        } catch (IndexOutOfBoundsException e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        area.setText(area.getText() + "\r\nФайл отправлен в корзину!\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 5: {
                                        area.setText("/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                        break;
                                    case 6: {
                                        if (!line.equals("bucket -c"))
                                            area.setText(area.getText() + "\r\n" + oper.show_files(true) + "\r\n/home/$: ");
                                        else {
                                            oper.clear_bucket();
                                            area.setText(area.getText() + "\r\nКорзина успешно очищена!\r\n/home/$: ");
                                        }
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 7: {
                                        String name, pass, group;
                                        try {
                                            name = line.substring(normIndexOf(line, ' ', 1) + 1,
                                                    normIndexOf(line, ' ', 2));
                                            if (name.equals("") || name.equals(" "))
                                                throw new UniExp("Имя пользователя не может быть пустым!");
                                            pass = line.substring(normIndexOf(line, ' ', 2) + 1,
                                                    normIndexOf(line, ' ', 3));
                                            if (pass.equals("") || pass.equals(" "))
                                                throw new UniExp("Пароль не может быть пустым!");
                                            group = line.substring(normIndexOf(line, ' ', 3) + 1);
                                            if (group.equals("") || group.equals(" "))
                                                throw new UniExp("Укажите группу!");
                                        } catch (IndexOutOfBoundsException e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        oper.create_us_gr(true, name, pass, group);
                                        area.setText(area.getText() + "\r\nПользователь " + name + " успешно добавлен!" +
                                                "\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 8: {
                                        String group;
                                        try {
                                            group = line.substring(line.indexOf(" ") + 1);
                                            if (group.equals("") || group.equals(" "))
                                                throw new UniExp("Название группы не может быть пустым!");
                                        } catch (IndexOutOfBoundsException e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        oper.create_us_gr(false, group, "", "");
                                        area.setText(area.getText() + "\r\nГруппа " + group + " успешно создана!" +
                                                "\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 9: {
                                        String name, pass;
                                        try {
                                            name = line.substring(line.indexOf(" ") + 1, normIndexOf(line, ' ', 2));
                                            if (name.equals("") || name.equals(" "))
                                                throw new UniExp("Имя пользователя не может быть пустым!");
                                            pass = line.substring(normIndexOf(line, ' ', 2) + 1);
                                            if (pass.equals("") || pass.equals(" "))
                                                throw new UniExp("Пароль не может быть пустым!");
                                        } catch (IndexOutOfBoundsException e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        area.setText(area.getText() + "\r\n" + oper.change_user(name, pass) +
                                                "\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 10: {
                                        String name, group;
                                        try {
                                            name = line.substring(line.indexOf(" ") + 1, normIndexOf(line, ' ', 2));
                                            if (name.equals("") || name.equals(" "))
                                                throw new UniExp("Имя пользователя не может быть пустым!");
                                            group = line.substring(normIndexOf(line, ' ', 2) + 1);
                                            if (group.equals("") || group.equals(" "))
                                                throw new UniExp("Укажите группу!");
                                        } catch (IndexOutOfBoundsException e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        oper.change_gr(name, group);
                                        area.setText(area.getText() + "\r\nПользователь " + name + " переведен в группу "
                                                + group + ".\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 11: {
                                        String old_name, new_name;
                                        try {
                                            old_name = line.substring(line.indexOf(" ") + 1, normIndexOf(line, ' ', 2));
                                            if (old_name.equals("") || old_name.equals(" "))
                                                throw new UniExp("Укажите старое имя файла!");
                                            new_name = line.substring(normIndexOf(line, ' ', 2) + 1);
                                            if (new_name.equals("") || new_name.equals(" "))
                                                throw new UniExp("Укажите новое имя файла!");
                                        } catch (IndexOutOfBoundsException e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        oper.rename_file(old_name, new_name);
                                        area.setText(area.getText() + "\r\nФайл переименован!\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 12: {
                                        String name, new_attrs;
                                        try {
                                            name = line.substring(line.indexOf(" ") + 1, normIndexOf(line, ' ', 2));
                                            if (name.equals("") || name.equals(" "))
                                                throw new UniExp("Имя файла не может быть пустым!");
                                            new_attrs = line.substring(normIndexOf(line, ' ', 2) + 1);
                                            if (new_attrs.equals("") || new_attrs.equals(" "))
                                                throw new UniExp("Укажите атрибуты!");
                                        } catch (IndexOutOfBoundsException e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        oper.change_attrs(name, new_attrs);
                                        area.setText(area.getText() + "\r\nАтрибуты файла " + name + " изменены!\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 13: {
                                        String name, new_rights;
                                        try {
                                            name = line.substring(line.indexOf(" ") + 1, normIndexOf(line, ' ', 2));
                                            if (name.equals("") || name.equals(" "))
                                                throw new UniExp("Имя файла не может быть пустым!");
                                            new_rights = line.substring(normIndexOf(line, ' ', 2) + 1);
                                            if (new_rights.equals("") || new_rights.equals(" "))
                                                throw new UniExp("Укажите новые права доступа!");
                                        } catch (IndexOutOfBoundsException e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        oper.change_rights(name, new_rights);
                                        area.setText(area.getText() + "\r\nПрава на файл " + name + " изменены!\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 14: {
                                        try {
                                            String name = line.substring(line.indexOf(" ") + 1);
                                            if (name.equals("") || name.equals(" "))
                                                throw new UniExp("Имя файла не может быть пустым!");
                                            oper.restore_file(name);
                                            area.setText(area.getText() + "\r\nФайл " + name + " восстановлен!\r\n/home/$: ");
                                            area.positionCaret(area.getText().length());
                                        } catch (IndexOutOfBoundsException e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                    }
                                    break;
                                    case 15: {
                                        area.setText(area.getText() + "\r\n" + oper.getUsers(true) + "\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 16: {
                                        area.setText(area.getText() + "\r\n" + oper.getUsers(false) + "\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 17: {
                                        area.setText(area.getText() + "\r\n" + oper.getGroups() + "/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 18: {
                                        String name;
                                        try {
                                            name = line.substring(line.indexOf(" ") + 1);
                                            if (name.equals("") || name.equals(" "))
                                                throw new UniExp("Имя пользователя не может быть пустым!");
                                        } catch (IndexOutOfBoundsException e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                        alert.setContentText("При удалении пользователя все его файлы тоже будут удалены. " +
                                                "Вы уверены?");
                                        ButtonType delete = new ButtonType("Удалить");
                                        ButtonType not_delete = new ButtonType("Пусть живет");
                                        alert.getButtonTypes().setAll(delete, not_delete);
                                        Optional<ButtonType> result = alert.showAndWait();
                                        if (result.get().equals(delete)) {
                                            oper.del_us_gr(name, false);
                                            area.setText(area.getText() + "\r\nПользователь " + name
                                                    + " удален из системы!\r\n/home/$: ");
                                        } else
                                            area.setText(area.getText() + "\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 19: {
                                        String name;
                                        try {
                                            name = line.substring(line.indexOf(" ") + 1);
                                            if (name.equals("") || name.equals(" "))
                                                throw new UniExp("Имя пользователя не может быть пустым!");
                                        } catch (IndexOutOfBoundsException e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                        alert.setContentText("При удалении группы все её пользователи и файлы тоже будут удалены. " +
                                                "Вы уверены?");
                                        ButtonType delete = new ButtonType("Удалить");
                                        ButtonType not_delete = new ButtonType("Пусть живет");
                                        alert.getButtonTypes().setAll(delete, not_delete);
                                        Optional<ButtonType> result = alert.showAndWait();
                                        if (result.get().equals(delete)) {
                                            oper.del_us_gr(name, true);
                                            area.setText(area.getText() + "\r\nГруппа " + name
                                                    + " удалена из системы!\r\n/home/$: ");
                                        } else
                                            area.setText(area.getText() + "\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 20: {
                                        String old_name, new_name, new_pass;
                                        try {
                                            old_name = line.substring(line.indexOf(" ") + 1, normIndexOf(line, ' ', 2));
                                            new_name = line.substring(normIndexOf(line, ' ', 2) + 1,
                                                    normIndexOf(line, ' ', 3));
                                            new_pass = line.substring(normIndexOf(line, ' ', 3) + 1);
                                        } catch (IndexOutOfBoundsException ioe) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        oper.rename_us_gr(true, old_name, new_name, new_pass);
                                        area.setText(area.getText() + "\r\nИмя пользователя успешно изменено на "
                                                + new_name + "!\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 21: {
                                        String old_name, new_name;
                                        try {
                                            old_name = line.substring(line.indexOf(" ") + 1, normIndexOf(line, ' ', 2));
                                            new_name = line.substring(normIndexOf(line, ' ', 2) + 1);
                                        } catch (IndexOutOfBoundsException ioe) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        oper.rename_us_gr(false, old_name, new_name, "");
                                        area.setText(area.getText() + "\r\nНазвание группы успешно изменено на "
                                                + new_name + "!\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 22: {
                                        String username, new_pass;
                                        try {
                                            username = line.substring(line.indexOf(" ") + 1, normIndexOf(line, ' ', 2));
                                            new_pass = line.substring(normIndexOf(line, ' ', 2) + 1);
                                        } catch (IndexOutOfBoundsException ioe) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        oper.change_pass(username, new_pass);
                                        area.setText(area.getText() + "\r\nПароль пользователя "
                                                + username + " успешно изменен!\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 23: {
                                        String original, copyto;
                                        try {
                                            original = line.substring(line.indexOf(" ") + 1, normIndexOf(line, ' ', 2));
                                            copyto = line.substring(normIndexOf(line, ' ', 2) + 1);
                                        } catch (IndexOutOfBoundsException ioe) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        oper.copy_file(original, copyto);
                                        area.setText(area.getText() + "\r\nКопирование успешно завершено!\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 24: {
                                        oper.sign_out();
                                        area.setText(area.getText() + "\r\nВы вышли из системы!\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 25: {
                                        ((Stage) area.getScene().getWindow()).close();
                                        Stage stage = new Stage();
                                        FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
                                        Parent root = loader.load();
                                        stage.setScene(new Scene(root, 300, 275));
                                        stage.show();
                                    }
                                    break;
                                    case 26: {
                                        if (line.equals("ps -l") || line.startsWith("ps -l "))
                                            oper.launch_process(true);
                                        else
                                            oper.launch_process(false);
                                        area.setText(area.getText() + "\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 27: {
                                        short CPU_burst;
                                        try {
                                            CPU_burst = Short.parseShort(line.substring(line.indexOf(" ") + 1));
                                        } catch (IndexOutOfBoundsException ioe) {
                                            throw new UniExp("Неверный формат!");
                                        } catch (Exception e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        oper.create_proc(CPU_burst);
                                        area.setText(area.getText() + "\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 28: {
                                        short PID;
                                        boolean instant;
                                        try {
                                            if(line.startsWith("kill -9 ")) {
                                                PID = Short.parseShort(line.substring(normIndexOf(line, ' ', 2)
                                                        + 1));
                                                instant = true;
                                            } else {
                                                PID = Short.parseShort(line.substring(line.indexOf(" ") + 1));
                                                instant = false;
                                            }
                                        } catch (IndexOutOfBoundsException ioe) {
                                            throw new UniExp("Неверный формат!");
                                        } catch (Exception e) {
                                            throw new UniExp("Неверный формат!");
                                        }
                                        oper.remove_proc(PID, instant);
                                        area.setText(area.getText() + "\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                    case 29: {
                                        short PID;
                                        byte correction;
                                        try {
                                            PID = Short.parseShort(line.substring(line.indexOf(" ") + 1,
                                                    normIndexOf(line, ' ', 2)));
                                            correction = Byte.parseByte(line.substring(normIndexOf(line, ' ', 2)
                                                    + 1));
                                        } catch(IndexOutOfBoundsException ioe){
                                            throw new UniExp("Неверный формат!");
                                        } catch (Exception e){
                                            throw new UniExp("Неверный формат!");
                                        }
                                        oper.change_ps_prio(PID, correction);
                                        area.setText(area.getText() + "\r\n/home/$: ");
                                        area.positionCaret(area.getText().length());
                                    }
                                    break;
                                }

                            } catch (UniExp ue) {
                                area.setText(area.getText() + "\r\n" + ue.getMessage() + "\r\n/home/$: ");
                                area.positionCaret(area.getText().length());
                            }
                            event.consume();
                        } else if (event.getCode() == KeyCode.UP) {
                            if (last_indic >= 0) {
                                area.setText(area.getText().substring(0, area.getText().lastIndexOf("\n"))
                                        + "\r\n/home/$: " + last_commands.get(last_indic));
                                area.positionCaret(area.getText().length());
                                last_indic--;
                                if (last_indic == -1)
                                    last_indic = last_commands.size() - 1;
                            }
                            event.consume();
                        }
                    } else {
                        if (event.getCode() == KeyCode.ESCAPE) {
                            if (current == curr.write)
                                oper.write_file(chosen_file.toString(), area.getText(), false);
                            else
                                oper.write_file(chosen_file.toString(), area.getText(), true);
                            area.setText(bak.toString() + "\r\nЗапись успешно выполнена!\r\n/home/$: ");
                            area.positionCaret(area.getText().length());
                            bak.delete(0, bak.length());
                            chosen_file.delete(0, chosen_file.length());
                            current = curr.normal;
                        }
                    }
                } catch (IOException ioe) {
                    exception(event, ioe);
                } catch (UniExp ue) {
                    exception(event, ue);
                } catch (Exception e) {
                    exception(event, e);
                }
            }
        });
    }

    void exception(KeyEvent event, Exception e) {
        e.printStackTrace();
        if (bak.toString().equals(""))
            area.setText(area.getText() + "\r\nОшибка: " + e.getMessage() + "\r\n/home/$: ");
        else {
            area.setText(bak.toString() + "\r\nОшибка: " + e.getMessage() + "\r\n/home/$: ");
            bak.delete(0, bak.length());
            chosen_file.delete(0, chosen_file.length());
        }
        if (current != curr.normal)
            current = curr.normal;
        area.positionCaret(area.getText().length());
        event.consume();
    }
}
