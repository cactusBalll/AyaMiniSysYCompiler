package backend.instr;

import backend.MCBlock;
import backend.regs.PReg;
import backend.regs.Reg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MCInstrB extends MCInstr{

    public Type type;
    public Reg s;
    public Reg t;
    public MCBlock target;

    @Override
    public List<Reg> getDef() {
        return new ArrayList<>();
    }

    @Override
    public List<Reg> getUse() {
        List<Reg> ret = new ArrayList<>();
        ret.add(s);
        ret.add(t);
        return ret;
    }

    @Override
    public void allocate(Reg vReg, Reg pReg) {
        if (s == vReg) {
            s = pReg;
        }
        if (t == vReg) {
            t = pReg;
        }
    }

    public enum Type{
        beq,
        bgez,
        bgtz,
        blez,
        bltz,
        bne
    }

    public MCInstrB(Type type, Reg s, Reg t, MCBlock target) {
        this.type = type;
        this.s = s;
        this.t = t;
        this.target = target;
    }

    @Override
    public String toString() {
        if (t != null) {
            return String.format("%s %s,%s,%s", type, s,t,target.label.name);
        } else {
            return String.format("%s %s,%s", type,s,target.label.name);
        }

    }
}
