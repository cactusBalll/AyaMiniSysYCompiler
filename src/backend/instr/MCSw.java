package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;

import java.util.HashSet;
import java.util.Set;

/**
 * Mem[Rs + offset] = Rt
 */
public class MCSw extends MCInstr{
    public int numOffset;
    public Reg s;
    public Reg t;
    public Label offset;

    public MCSw(Reg s, Reg t, Label offset) {
        this.s = s;
        this.t = t;
        this.offset = offset;
    }

    public MCSw(Reg s, Reg t, int numOffset) {
        this.s = s;
        this.t = t;
        this.numOffset = numOffset;
        offset = null;
    }

    public MCSw(int numOffset, Reg s, Reg t, Label offset) {
        this.numOffset = numOffset;
        this.s = s;
        this.t = t;
        this.offset = offset;
    }

    @Override
    public String toString() {
        if (offset != null) {
            if (numOffset == 0) {
                return String.format("sw %s,%s(%s)",t,offset.name,s);
            } else {
                return String.format("sw %s,%s+%d(%s)",t,offset.name,numOffset,s);
            }
        } else {
            return String.format("sw %s,%d(%s)",t,numOffset,s);
        }

    }


    @Override
    public Set<Reg> getUse() {
        Set<Reg> ret = new HashSet<>();
        ret.add(t);
        ret.add(s);
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
}
