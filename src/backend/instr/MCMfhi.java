package backend.instr;

import backend.regs.Reg;

public class MCMfhi extends MCInstr{
    public Reg d;

    public MCMfhi(Reg d) {
        this.d = d;
    }

    @Override
    public String toString() {
        return "mfhi " + d.toString();
    }
}
