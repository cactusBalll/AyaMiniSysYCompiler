package backend.instr;

import backend.regs.Reg;

/**
 * Rt = Mem[Rs + offset]
 */
public class MCLw extends MCInstr{
    public Reg s;
    public Reg t;
    public Label offset;
    public int numOffset = 0;

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

    @Override
    public String toString() {
        if (offset != null) {
            return String.format("lw %s,%s(%s)",t,offset,s);
        } else {
            return String.format("lw %s,%d(%s)",t,numOffset,s);
        }

    }
}
