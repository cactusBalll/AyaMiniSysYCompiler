package backend.instr;

import backend.regs.Reg;

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

    @Override
    public String toString() {
        if (offset != null) {
            return String.format("sw %s,%s(%s)",t,offset,s);
        } else {
            return String.format("sw %s,%d(%s)",t,numOffset,s);
        }

    }
}
