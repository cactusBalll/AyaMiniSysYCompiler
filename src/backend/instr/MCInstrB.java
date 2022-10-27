package backend.instr;

import backend.MCBlock;
import backend.regs.Reg;

import java.util.HashSet;
import java.util.Set;

public class MCInstrB extends MCInstr{

    public Type type;
    public Reg s;
    public Reg t;
    public MCBlock target;

    @Override
    public Set<Reg> getDef() {
        return new HashSet<>();
    }

    @Override
    public Set<Reg> getUse() {
        Set<Reg> ret = new HashSet<>();
        ret.add(s);
        ret.add(t);
        return ret;
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
            return String.format("%s %s,%s,%s", type, s,t,target);
        } else {
            return String.format("%s %s,%s", type,s,target);
        }

    }
}
