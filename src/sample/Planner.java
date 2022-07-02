package sample;

import PlannerComponents.Process;
import PlannerComponents.Queue;
import Utilities.UniExp;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.util.Random;

public class Planner {
    @FXML
    volatile TextArea firstqueue, secondqueue, thirdqueue, psl;
    @FXML
    volatile TextField queue1, queue2, queue3, header;
    volatile TextArea[] areas;
    volatile TextField[] queue_heads;
    volatile Queue[] queues;
    volatile Process cur_proc;
    volatile String[] begin_users;
    volatile Process new_proc;
    volatile short delete_PID;
    volatile short prio_ch_PID;
    volatile boolean instant_delete;
    volatile int cur_queue;
    volatile short cur_proc_num;
    volatile boolean freezed;
    volatile boolean closing;
    volatile Object synchronizer;
    volatile boolean first = true;

    public void setBegin_users(String[] begin_users) {
        this.begin_users = begin_users;
    }

    void start_fill() {
        queues[0].insert(new Process(form_PID(), begin_users[0], (byte) 0, true, (short) 500, set_prio((short) 500, (byte) 0)));
        queues[0].insert(new Process(form_PID(), begin_users[0], (byte) 0, true, (short) 100, set_prio((short) 100, (byte) 0)));
        queues[0].insert(new Process(form_PID(), begin_users[0], (byte) 0, true, (short) 250, set_prio((short) 250, (byte) 0)));
        queues[0].insert(new Process(form_PID(), begin_users[0], (byte) 0, true, (short) 50, set_prio((short) 50, (byte) 0)));
        queues[0].insert(new Process(form_PID(), begin_users[0], (byte) 0, true, (short) 700, set_prio((short) 700, (byte) 0)));
        queues[1].insert(new Process(form_PID(), begin_users[0], (byte) 0, true, (short) 500, set_prio((short) 500, (byte) 1)));
        queues[1].insert(new Process(form_PID(), begin_users[1], (byte) 1, false, (short) 700, set_prio((short) 700, (byte) 1)));
        queues[1].insert(new Process(form_PID(), begin_users[1], (byte) 1, false, (short) 650, set_prio((short) 650, (byte) 1)));
        queues[1].insert(new Process(form_PID(), begin_users[0], (byte) 0, true, (short) 1000, set_prio((short) 1000, (byte) 1)));
        queues[1].insert(new Process(form_PID(), begin_users[1], (byte) 1, false, (short) 200, set_prio((short) 200, (byte) 1)));
        queues[2].insert(new Process(form_PID(), begin_users[1], (byte) 1, false, (short) 340, set_prio((short) 340, (byte) 2)));
        queues[2].insert(new Process(form_PID(), begin_users[2], (byte) 2, false, (short) 120, set_prio((short) 120, (byte) 2)));
        queues[2].insert(new Process(form_PID(), begin_users[2], (byte) 2, false, (short) 500, set_prio((short) 500, (byte) 2)));
        queues[2].insert(new Process(form_PID(), begin_users[2], (byte) 2, false, (short) 640, set_prio((short) 640, (byte) 2)));
        queues[2].insert(new Process(form_PID(), begin_users[1], (byte) 1, false, (short) 100, set_prio((short) 100, (byte) 2)));

    }

    int[] find_proc(short PID) throws UniExp {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < queues[i].getProcessCount(); j++)
                if (queues[i].getProcess(j).getPID() == PID)
                    return new int[]{i, j};
        throw new UniExp("Процесса с идентификатором " + PID + " не существует!");
    }

    public void set_del_params(short PID, byte cur_UID, boolean admin, boolean instant) throws UniExp {
        int[] found = find_proc(PID);
        if (admin || (cur_UID == queues[found[0]].getProcess(found[1]).getUID() &&
                !queues[found[0]].getProcess(found[1]).isAdmin())) {
            delete_PID = PID;
            instant_delete = instant;
        } else throw new UniExp("Недостаточно прав!");
    }

    public void set_user_corr(short PID, byte value, boolean admin, byte cur_UID) throws UniExp {
        int[] found = find_proc(PID);
        if (!(admin || (cur_UID == queues[found[0]].getProcess(found[1]).getUID() &&
                !queues[found[0]].getProcess(found[1]).isAdmin()))) throw new UniExp("Недостаточно прав!");
        byte new_prio;
        //boolean changed = false;
        try {
            new_prio = (byte) (queues[found[0]].getProcess(found[1]).getPrio() + value);
        } catch (Exception e) {
            throw new UniExp("Слишком высокая поправка!");
        }
        if (new_prio > queues[found[0]].getMax_prio())
            throw new UniExp("Слишком высокая поправка!");
        if (new_prio < queues[found[0]].getMin_prio())
            throw new UniExp("Слишком низкая поправка!");
        prio_ch_PID = queues[found[0]].getProcess(found[1]).getPID();
        //Process corrected = queues[found[0]].getProcess(found[1]);
        queues[found[0]].getProcess(found[1]).setUser_corr((byte) (queues[found[0]]
                .getProcess(found[1]).getUser_corr() + value));
        /*if (cur_proc_num == found[1] && cur_queue == found[0])
            //if (!freezed) {
            //    freezed = true;
            //    changed = true;
            synchronized (synchronizer) {
                try {
                    synchronizer.wait();
                } catch (InterruptedException e) {
                    System.out.println("Interruprted...");
                }
            }*/
        //}
        //queues[found[0]].insert(corrected);
        //queues[found[0]].remove_proc(found[1], true);
        // if (changed) {
        //     freezed = false;
        //     synchronized (synchronizer) {
        //         synchronizer.notify();
        //     }
        // }
    }

    void change_queue(Process pr, boolean up) {
        try {
            int[] pos = find_proc(pr.getPID());
            byte system_corr;
            if (up) {
                if (pos[0] == 0)
                    return;
                pos[0]--;
                if (queues[pos[0]].getProcessCount() == Short.MAX_VALUE)
                    return;
                system_corr = (byte) (pr.getPrio() - queues[pos[0]].getMax_prio());
                pr.setSystem_corr((byte) (pr.getSystem_corr() - system_corr));
                queues[pos[0]].insert(pr);
                queues[pos[0] + 1].remove_proc(pos[1], true);
            } else {
                if (pos[0] == 2 || (pr.isAdmin() && pos[0] == 1))
                    change_queue(pr, true);
                else {
                    pos[0]++;
                    if (queues[pos[0]].getProcessCount() == Short.MAX_VALUE)
                        return;
                    system_corr = (byte) (queues[pos[0]].getMax_prio() - pr.getPrio());
                    pr.setSystem_corr((byte) (pr.getSystem_corr() + system_corr));
                    queues[pos[0]].insert(pr);
                    queues[pos[0] - 1].remove_proc(pos[1], true);
                }
            }
        } catch (UniExp ue) {
        }
    }

    short form_PID() {
        if (all_process_count() == 0)
            return 0;
        short max_PID = 0;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < queues[i].getProcessCount(); j++) {
                if (max_PID < queues[i].getProcess(j).getPID())
                    max_PID = queues[i].getProcess(j).getPID();
            }
        max_PID++;
        return max_PID;
    }

    byte set_prio(short CPU_burst, byte queue_num) {
        if (CPU_burst <= queues[queue_num].getTime_quant())
            return queues[queue_num].getMin_prio();
        int k = 1;
        for (int i = queues[queue_num].getMin_prio() + 1; i < queues[queue_num].getMax_prio(); i++) {
            if (CPU_burst >= queues[queue_num].getTime_quant() + k * 10 && CPU_burst
                    <= queues[queue_num].getTime_quant() + k * 10 + 20)
                return (byte) i;
            k++;
        }
        return queues[queue_num].getMax_prio();
    }

    public void set_new_process(String user_name, byte UID, boolean admin, short CPU_burst, byte queue_num) throws UniExp {
        while (queues[queue_num].getProcessCount() == Short.MAX_VALUE) {
            queue_num++;
            if (queue_num == queues.length)
                throw new UniExp("Превышено макс. число процессов!");
        }
        new_proc = new Process(form_PID(), user_name, UID, admin, CPU_burst, set_prio(CPU_burst, queue_num));
    }

    int all_process_count() {
        int count = 0;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < queues[i].getProcessCount(); j++)
                count++;
        return count;
    }

    int find_queue(byte prio) {
        if (prio >= 0 && prio <= 9)
            return 0;
        else if (prio >= 10 && prio <= 30)
            return 1;
        else if (prio >= 31 && prio <= 50)
            return 2;
        return -1;
    }

    void restart() {
        if (all_process_count() == 0)
            start_fill();
        for (int i = 0; i < queues.length; i++)
            queues[i].setCur_proc((short) 0);
        planning();
    }

    int[] proc_operations(int bq, int bc, int cntr, int prc_cnt, boolean running) throws UniExp {
        int bak_queue = bq;
        int bak_counter = bc;
        int counter = cntr;
        int proc_count = prc_cnt;
        if (prio_ch_PID != -1) {
            int[] prio = find_proc(prio_ch_PID);
            Process with_changed_prio = queues[prio[0]].getProcess(prio[1]);
            queues[prio[0]].remove_proc(prio[1], true);
            queues[prio[0]].insert(with_changed_prio);
            prio_ch_PID = -1;
        }
        if (new_proc != null) {
            if (find_queue(new_proc.getPrio()) < cur_queue) {
                cur_proc.setState((byte) 0);
                cur_proc.newPass();
                if (cur_proc.getPass() > 2) {
                    change_queue(cur_proc, true);
                    cur_proc.clearPass();
                }
                cur_proc = new_proc;
                cur_proc.setState((byte) 5);
                bak_queue = cur_queue;
                bak_counter = counter;
                counter = 0;
                queues[cur_queue].setCur_proc(cur_proc_num);
                cur_queue = find_queue(new_proc.getPrio());
            }
            queues[find_queue(new_proc.getPrio())].insert(new_proc);
            new_proc = null;
            proc_count++;
        }
        if (delete_PID != -1) {
            if (instant_delete) {
                if (delete_PID == cur_proc.getPID()) {
                            /*try {
                                queues[cur_queue].remove_proc(delete_PID, true);
                            } catch (UniExp ue) {

                            }*/
                    if (bak_queue == -1) {
                        cur_proc_num++;
                        if (cur_proc_num == queues[cur_queue].getProcessCount())
                            cur_proc_num = 0;
                        cur_proc = queues[cur_queue].getProcess(cur_proc_num);
                    } else {
                        cur_queue = bak_queue;
                        counter = bak_counter;
                        cur_proc_num = queues[cur_queue].getCur_proc();
                        while (cur_proc_num >= queues[cur_queue].getProcessCount())
                            cur_proc_num--;
                        cur_proc = queues[cur_queue].getProcess(cur_proc_num);
                        cur_proc.setState((byte) 9);
                        bak_counter = -1;
                        bak_queue = -1;
                    }
                }
                try {
                    int[] deleting = find_proc(delete_PID);
                    queues[deleting[0]].remove_proc(deleting[1], true);
                } catch (UniExp ue) {
                }
            } else if (delete_PID != cur_proc.getPID() || (delete_PID == cur_proc.getPID() && !running)) {
                try {
                    int[] deleting = find_proc(delete_PID);
                    queues[deleting[0]].remove_proc(deleting[1], false);
                } catch (UniExp ue) {
                }
            }
            if (!(delete_PID == cur_proc.getPID() && !instant_delete && running)) {
                delete_PID = -1;
                instant_delete = false;
                proc_count--;
            }
        }
        return new int[]{bak_queue, bak_counter, counter, proc_count};
    }

    public void planning() {
        cur_queue = 0;
        cur_proc_num = 0;
        new_proc = null;
        delete_PID = -1;
        prio_ch_PID = -1;
        instant_delete = false;
        closing = false;
        freezed = false;
        int proc_count = all_process_count();
        int quant_pass = 0;
        int counter = 0;
        int bak_queue = -1;
        int bak_counter = -1;
        boolean changed = true;
        cur_proc = null;
        try {
            while (proc_count > 0) {
                try {
                    while (queues[cur_queue].getProcessCount() == 0 || queues[cur_queue].only_zombies_left()) {
                        cur_queue++;
                        if (cur_queue == queues.length)
                            cur_queue = 0;
                        changed = true;
                    }
                    if (changed) {
                        cur_proc_num = queues[cur_queue].getCur_proc();
                        changed = false;
                    }
                    while (cur_proc_num >= queues[cur_queue].getProcessCount()) {
                        cur_proc_num--;
                    }
                    if (cur_proc != null) {
                        if (cur_proc.getPID() == queues[cur_queue].getProcess(cur_proc_num).getPID()) {
                            cur_proc_num++;
                            if (cur_proc_num == queues[cur_queue].getProcessCount())
                                cur_proc_num = 0;
                        }
                    }
                    if (first) {
                        cur_proc_num = -1;
                        first = false;
                    }
                    cur_proc = queues[cur_queue].getProcess(cur_proc_num);
                    if (cur_proc.getState() == 0) {
                        Thread.sleep(1000);
                        cur_proc.setState((byte) 9);
                        cur_proc_num++;
                        if (cur_proc_num == queues[cur_queue].getProcessCount())
                            cur_proc_num = 0;
                        int after[] = proc_operations(bak_queue, bak_counter, counter, proc_count, false);
                        bak_queue = after[0];
                        bak_counter = after[1];
                        counter = after[2];
                        proc_count = after[3];
                    } else if (cur_proc.getState() == 9) {
                        cur_proc.setState((byte) 5);
                        counter = 0;
                        while (cur_proc.getCPU_burst() != 0 && counter < queues[cur_queue].getTime_quant()) {
                            if (freezed) {
                                synchronized (synchronizer) {
                                    // synchronizer.notify();
                                    synchronizer.wait();
                                }
                            }
                            /*if(prio_ch_PID != -1){
                                int[] prio = find_proc(prio_ch_PID);
                                Process with_changed_prio = queues[prio[0]].getProcess(prio[1]);
                                queues[prio[0]].remove_proc(prio[1], true);
                                queues[prio[0]].insert(with_changed_prio);
                                prio_ch_PID = -1;
                            }*/
                            //while (freezed)
                            //     Thread.sleep(3000);
                            if (closing)
                                throw new UniExp("");
                            cur_proc.reduceBurst();
                            counter++;
                            int[] after = proc_operations(bak_queue, bak_counter, counter, proc_count, true);
                            bak_queue = after[0];
                            bak_counter = after[1];
                            counter = after[2];
                            proc_count = after[3];
                            update(psl.isVisible());
                            Thread.sleep(1000);
                        }
                        if (delete_PID != -1) {
                            try {
                                int[] deleting = find_proc(delete_PID);
                                queues[deleting[0]].remove_proc(deleting[1], false);
                            } catch (UniExp ue) {
                            }
                            delete_PID = -1;
                            proc_count--;
                        }
                        if (cur_proc.getCPU_burst() == 0) {
                            try {
                                int[] deleting = find_proc(cur_proc.getPID());
                                queues[deleting[0]].remove_proc(deleting[1], true);
                                proc_count--;
                            } catch (UniExp ue) {
                            }
                        } else {
                            if (cur_proc.getState() != -1) {
                                if (cur_proc.getPrio() >
                                        (queues[cur_queue].getMax_prio() - queues[cur_queue].getMin_prio()) / 2)
                                    change_queue(cur_proc, false);
                                if (new Random().nextInt(10) + 1 > 7)
                                    cur_proc.setState((byte) 0);
                                else
                                    cur_proc.setState((byte) 9);
                            }
                        }
                        if (bak_queue != -1) {
                            cur_queue = bak_queue;
                            bak_queue = -1;
                            bak_counter = -1;
                            if (queues[cur_queue].getProcess(cur_proc_num).getState() != -1)
                                queues[cur_queue].getProcess(cur_proc_num).setState((byte) 9);
                        } else {
                            quant_pass++;
                            if (quant_pass == queues[cur_queue].getLimit() || queues[cur_queue].getProcessCount() == 0) {
                                if (cur_proc_num + 1 >= queues[cur_queue].getProcessCount())
                                    queues[cur_queue].setCur_proc((short) 0);
                                else
                                    queues[cur_queue].setCur_proc(cur_proc_num);
                                cur_queue++;
                                if (cur_queue == 3)
                                    cur_queue = 0;
                                for (int i = 0; i < 3; i++) {
                                    queues[i].setCur_proc((short) 0);
                                    queues[i].clear();
                                }
                                quant_pass = 0;
                                changed = true;
                            } else {
                                cur_proc_num++;
                                if (cur_proc_num == queues[cur_queue].getProcessCount())
                                    cur_proc_num = 0;
                            }
                        }
                    }
                    if (proc_count == 0) {
                        start_fill();
                        proc_count = all_process_count();
                    }
                } catch (Exception e) {
                    if (closing)
                        throw new UniExp("");
                    else {
                        e.printStackTrace();
                        throw new UniExp("Restart");
                    }
                }
            }
        } catch (UniExp e) {
            if (e.getMessage().equals("Restart")) {
                if (header.getScene().getWindow().isShowing()) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Подождите!");
                            alert.setContentText("Обнаружены неполадки в работе планировщика. Выполняется перезапуск...");
                            alert.show();
                        }
                    });
                }
                restart();
            }
        }
    }

    public void update(boolean long_ps) {
        if (all_process_count() > 0) {
            StringBuffer sb = new StringBuffer();
            boolean do_not_include;
            if (long_ps) {
                if (queue_heads[0].isVisible()) {
                    for (int i = 0; i < queue_heads.length; i++) {
                        queue_heads[i].setVisible(false);
                        areas[i].setVisible(false);
                    }
                    psl.setVisible(true);
                    header.setText("Процессы");
                }
                psl.clear();
                if (cur_proc != null)
                    sb.append(cur_proc.get_long_info() + "\r\nОчередь: " + cur_queue + "\r\n----------------------------------\r\n");
                for (int i = 0; i < queues.length; i++) {
                    for (int j = 0; j < queues[i].getProcessCount(); j++) {
                        do_not_include = false;
                        if (cur_proc != null) {
                            if (queues[i].getProcess(j).getPID() == cur_proc.getPID())
                                do_not_include = true;
                        }
                        if (!do_not_include)
                            sb.append(queues[i].getProcess(j).get_long_info() + "\r\nОчередь: " + i
                                    + "\r\n----------------------------------\r\n");
                    }
                }
                psl.setText(sb.toString());
            } else {
                if (psl.isVisible()) {
                    psl.setVisible(false);
                    for (int i = 0; i < queue_heads.length; i++) {
                        queue_heads[i].setVisible(true);
                        areas[i].setVisible(true);
                    }
                    header.setText("Очереди");
                }
                for (int i = 0; i < areas.length; i++) {
                    areas[i].clear();
                }
                for (int i = 0; i < queues.length; i++) {
                    if (i == cur_queue)
                        if (cur_proc != null)
                            sb.append(cur_proc.get_short_info() + "\r\n----------------------------------\r\n");
                    for (int j = 0; j < queues[i].getProcessCount(); j++) {
                        do_not_include = false;
                        if (cur_proc != null) {
                            if (queues[i].getProcess(j).getPID() == cur_proc.getPID())
                                do_not_include = true;
                        }
                        if (!do_not_include)
                            sb.append(queues[i].getProcess(j).get_short_info()
                                    + "\r\n----------------------------------\r\n");
                    }
                    areas[i].setText(sb.toString());
                    sb.delete(0, sb.length());
                }
            }
        }
    }

    public void setFreezed(boolean freezed) {
        this.freezed = freezed;
        if (!freezed) {
            header.setStyle("-fx-text-fill: #00ff00;");
            synchronized (synchronizer) {
                synchronizer.notify();
            }
        }
    }

    public void close() {
        closing = true;
        if (freezed) {
            freezed = false;
            synchronized (synchronizer) {
                synchronizer.notify();
            }
        }
    }

    @FXML
    public void initialize() {
        //update(false);
        //planning();
        synchronizer = new Object();
        areas = new TextArea[3];
        areas[0] = firstqueue;
        areas[1] = secondqueue;
        areas[2] = thirdqueue;
        queue_heads = new TextField[3];
        queue_heads[0] = queue1;
        queue_heads[1] = queue2;
        queue_heads[2] = queue3;
        /*for (int i = 0; i < queue_heads.length; i++)
            queue_heads[i].setStyle("-fx-control-inner-background:#000000; -fx-font-family: Consolas; " +
                    "-fx-text-fill: #00ff00;");
        header.setStyle("-fx-control-inner-background:#000000; -fx-font-family: Consolas; " +
                "-fx-text-fill: #00ff00;");
        for (int i = 0; i < areas.length; i++)
            areas[i].setStyle("-fx-control-inner-background:#000000; -fx-font-family: Consolas; " +
                    "-fx-highlight-fill: #00ff00; -fx-highlight-text-fill: #000000; " +
                    "-fx-text-fill: #00ff00;");
        psl.setStyle("-fx-control-inner-background:#000000; -fx-font-family: Consolas; " +
                "-fx-highlight-fill: #00ff00; -fx-highlight-text-fill: #000000; " +
                "-fx-text-fill: #00ff00;");*/
        header.setStyle("-fx-text-fill: #00ff00;");
        psl.setVisible(false);
        queues = new Queue[3];
        queues[0] = new Queue((short) 10, (byte) 15, (byte) 0, (byte) 9);
        queues[1] = new Queue((short) 8, (byte) 10, (byte) 10, (byte) 30);
        queues[2] = new Queue((short) 5, (byte) 8, (byte) 31, (byte) 50);
        header.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (freezed) {
                    freezed = false;
                    header.setStyle("-fx-text-fill: #00ff00;");
                    synchronized (synchronizer) {
                        synchronizer.notify();
                    }
                } else {
                    freezed = true;
                    header.setStyle("-fx-text-fill: #dc143c;");
                }
            }
        });
    }
}
