package PlannerComponents;

        import java.util.Random;

public class Process {
    private short PID;
    private String user_name;
    private byte UID;
    private boolean admin;
    private byte prio;
    private byte user_corr;
    private byte system_corr;
    private byte state;
    private short CPU_burst;
    private byte pass;

    public Process(short PID, String user, byte UID, boolean admin, short CPU_burst, byte prio) {
        this.PID = PID;
        this.user_name = user;
        this.UID = UID;
        this.prio = prio;
        this.CPU_burst = CPU_burst;
        this.admin = admin;
        if (new Random().nextInt(10) + 1 <= 7)
            state = 9;
        else
            state = 0;
        pass = 0;
    }

    public void setSystem_corr(byte system_corr) {
        this.system_corr = system_corr;
    }

    public byte getSystem_corr() {
        return system_corr;
    }

    public void setUser_corr(byte user_corr) {
        this.user_corr = user_corr;
    }

    public byte getUser_corr() {
        return user_corr;
    }

    public boolean isAdmin() {
        return admin;
    }

    public byte getUID() {
        return UID;
    }

    public byte getPrio() {
        return (byte) (prio + system_corr + user_corr);
    }

    public short getPID() {
        return PID;
    }

    public byte getState() {
        return state;
    }

    public String translate_state() {
        switch (state) {
            case 0:
                return "Ожидание";
            case 5:
                return "Выполнение";
            case 9:
                return "Готовность";
            case -1:
                return "Зомби";
        }
        return "";
    }

    public String get_short_info() {
        return "PID: " + PID + "\r\nПользователь: " + user_name + "\r\nИтоговый приоритет: " + getPrio()
                + "\r\nСостояние: " + translate_state() + "\r\nCPU burst: " + CPU_burst;
    }

    public String get_long_info() {
        return "PID: " + PID + "\r\nПользователь: " + user_name + "\r\nПриоритет: " + getPrio()
                + "\r\nПоправка к приоритету: " + user_corr + "\r\nСостояние: " + translate_state() + "\r\nCPU burst: "
                + CPU_burst;
    }

    public void reduceBurst() {
        CPU_burst--;
    }

    public short getCPU_burst() {
        return CPU_burst;
    }

    public void newPass() {
        pass++;
    }

    public void clearPass() {
        pass = 0;
    }

    public byte getPass() {
        return pass;
    }

    public void setState(byte state) {
        this.state = state;
    }
}
