package PlannerComponents;

import java.util.ArrayList;

public class Queue {
    private ArrayList<Process> processes;
    private short cur_proc;
    private short time_quant;
    private byte limit;
    private byte max_prio;
    private byte min_prio;

    public Queue(short time_quant, byte limit, byte min_prio, byte max_prio) {
        processes = new ArrayList<>();
        this.time_quant = time_quant;
        this.limit = limit;
        this.max_prio = max_prio;
        this.min_prio = min_prio;
    }

    public void insert(Process proc) {
        if (processes.size() == 0)
            processes.add(proc);
        else {
            int i = 0;
            while (i < processes.size() && processes.get(i).getPrio() < proc.getPrio())
                i++;
            while (i < processes.size() && processes.get(i).getCPU_burst() < proc.getCPU_burst())
                i++;
            if (i == processes.size())
                processes.add(proc);
            else
                processes.add(i, proc);
        }
    }

    public void remove_proc(int num, boolean instant) {
        if (instant)
            processes.remove(num);
        else
            processes.get(num).setState((byte) -1);
    }

    public boolean only_zombies_left(){
        if(processes.size() == 0)
            return false;
        int zombie_count = 0;
        for (int i = 0; i < processes.size(); i++){
            if(processes.get(i).getState() == -1)
                zombie_count++;
        }
        if(zombie_count == processes.size())
            return true;
        return false;
    }

    public void clear() {
        for (int i = 0; i < processes.size(); i++)
            if (processes.get(i).getState() == -1)
                processes.remove(i);
    }

    public void setCur_proc(short cur_proc) {
        this.cur_proc = cur_proc;
    }

    public short getCur_proc() {
        return cur_proc;
    }

    public Process getProcess(int num) {
        return processes.get(num);
    }

    public short getTime_quant() {
        return time_quant;
    }

    public byte getLimit() {
        return limit;
    }

    public byte getMax_prio() {
        return max_prio;
    }

    public byte getMin_prio() {
        return min_prio;
    }

    public int getProcessCount() {
        return processes.size();
    }

}
