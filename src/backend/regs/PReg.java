package backend.regs;

import Driver.AyaConfig;
import exceptions.BackEndErr;

import java.util.*;

public class PReg extends Reg{
    public String name;
    public int id;

    private PReg(String name, int id) {
        this.name = name;
        this.id = id;
    }

    private static List<PReg> regs = new ArrayList<PReg>(){
        {
            add(new PReg("zero", 0));
            add(new PReg("at",1)); //reserved,should not use
            add(new PReg("v0",2)); //reserved
            add(new PReg("v1", 3));
            add(new PReg("a0", 4)); //reserved
            add(new PReg("a1", 5)); //reserved
            add(new PReg("a2", 6)); //reserved
            add(new PReg("a3",7)); //reserved
            add(new PReg("t0",8));
            add(new PReg("t1",9));
            add(new PReg("t2",10));
            add(new PReg("t3",11));
            add(new PReg("t4",12));
            add(new PReg("t5",13));
            add(new PReg("t6",14));
            add(new PReg("t7",15));
            add(new PReg("s0", 16));
            add(new PReg("s1", 17));
            add(new PReg("s2", 18));
            add(new PReg("s3", 19));
            add(new PReg("s4", 20));
            add(new PReg("s5", 21));
            add(new PReg("s6", 22));
            add(new PReg("s7", 23));
            add(new PReg("t8",24));
            add(new PReg("t9",25));
            add(new PReg("k0",26));
            add(new PReg("k1",27));
            add(new PReg("gp", 28));
            add(new PReg("sp",29)); //reserved, only for stack
            add(new PReg("fp",30));
            add(new PReg("ra", 31)); //reserved, only for jr $ra
        }
    };
    public static Set<PReg> getSpRegs() {
        Set<PReg> ret = new HashSet<>();
        ret.add(regs.get(0));
        ret.add(regs.get(1));
        ret.add(regs.get(2));
        ret.add(regs.get(29));
        //ret.add(regs.get(30));
        ret.add(regs.get(31));
        return ret;
    }
    public static List<PReg> getTRegsWithoutAn() {
        List<PReg> ret = getTRegs();
        ret.remove(regs.get(4));
        ret.remove(regs.get(5));
        ret.remove(regs.get(6));
        ret.remove(regs.get(7));
        return ret;
    }

    public static List<PReg> getTRegs() {
        List<PReg> ret = new ArrayList<>(regs);
        ret.removeAll(getSpRegs());
        return ret;
    }

    public static List<PReg> getLessTRegs() {
        List<PReg> ret = new ArrayList<>();
        ret.add(regs.get(4));
        ret.add(regs.get(5));
        ret.add(regs.get(6));
        ret.add(regs.get(7));
        ret.add(regs.get(8));
        return ret;
    }
    public static List<PReg> getRegs() {
        return regs;
    }
    public static PReg getRegById(int id) throws BackEndErr {
        if (id < 0 || id > 31) {
            throw new BackEndErr();
        }
        return regs.get(id);
    }

    public static PReg getRegByName(String name) throws BackEndErr {
        for (PReg r :
                regs) {
            if (Objects.equals(r.name, name)) {
                return r;
            }
        }
        throw new BackEndErr();
    }

    @Override
    public String toString() {
        if (AyaConfig.USE_REG_NUMBER) {
            return "$" + id;
        } else {
            return "$" + name;
        }
    }
}
