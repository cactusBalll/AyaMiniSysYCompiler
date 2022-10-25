package backend.instr;

import backend.MCBlock;
import backend.regs.Reg;

public class MCInstrB extends MCInstr{

    public Type type;
    public Reg s;
    public Reg t;
    public MCBlock target;
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
