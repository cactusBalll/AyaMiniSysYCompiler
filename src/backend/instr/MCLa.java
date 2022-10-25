package backend.instr;

import backend.regs.Reg;

public class MCLa extends MCInstr{
    public Label target;
    public Reg s;

    public MCLa(Label target, Reg s) {
        this.target = target;
        this.s = s;
    }

    @Override
    public String toString() {
        return String.format("la %s,%s",s,target);
    }
}
