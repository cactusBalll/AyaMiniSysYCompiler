package backend.instr;

import backend.regs.Reg;

public class MCJr extends MCInstr{
    public Reg s;

    public MCJr(Reg s) {
        this.s = s;
    }

    @Override
    public String toString() {
        return "jr" + ' ' + s.toString();
    }
}
