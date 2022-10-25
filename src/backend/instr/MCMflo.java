package backend.instr;

import backend.regs.Reg;

public class MCMflo extends MCInstr{
    public Reg d;

    public MCMflo(Reg d) {
        this.d = d;
    }

    @Override
    public String toString() {
        return "mflo " + d.toString();
    }
}
