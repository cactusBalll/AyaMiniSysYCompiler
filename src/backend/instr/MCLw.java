package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;

import java.util.HashSet;
import java.util.Set;

/**
 * Rt = Mem[Rs + offset]
 */
public class MCLw extends MCInstr{
    public Reg s;
    public Reg t;
    public Label offset;
    public int numOffset = 0;

    public boolean isLoadArg = false; // 是否是在加载参数

    public MCLw(Reg s, Reg t, Label offset) {
        this.s = s;
        this.t = t;
        this.offset = offset;
    }

    public MCLw(Reg s, Reg t, int numOffset) {
        this.s = s;
        this.t = t;
        this.numOffset = numOffset;
        offset = null;
    }

    public MCLw(Reg s, Reg t, Label offset, int numOffset) {
        this.s = s;
        this.t = t;
        this.offset = offset;
        this.numOffset = numOffset;
    }

    @Override
    public String toString() {
        if (offset != null) {
            if (numOffset == 0) {
                return String.format("lw %s,%s(%s)",t,offset.name,s);
            } else {
                return String.format("lw %s,%s+%d(%s)",t,offset.name,numOffset,s);
            }

        } else {
            return String.format("lw %s,%d(%s)",t,numOffset,s);
        }

    }

    @Override
    public Set<Reg> getDef() {
        Set<Reg> ret = new HashSet<>();
        ret.add(t);
        return ret;
    }

    @Override
    public Set<Reg> getUse() {
        Set<Reg> ret = new HashSet<>();
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
