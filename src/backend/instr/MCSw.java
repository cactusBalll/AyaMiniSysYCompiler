package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mem[Rs + offset] = Rt
 */
public class MCSw extends MCInstr{
    public int numOffset;
    public Reg s;
    public Reg t;
    public Label offset;

    public boolean isLoadArg = false;

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
            if (s == PReg.getZero()) {
                if (numOffset == 0) {
                    return String.format("sw %s,%s",t,offset.name);
                } else {
                    return String.format("sw %s,%s+%d",t,offset.name,numOffset);
                }
            }
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
    public List<Reg> getUse() {
        List<Reg> ret = new ArrayList<>();
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
