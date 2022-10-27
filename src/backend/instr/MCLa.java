package backend.instr;

import backend.regs.Reg;

import java.util.HashSet;
import java.util.Set;

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

    @Override
    public Set<Reg> getDef() {
        Set<Reg> ret = new HashSet<>();
        ret.add(s);
        return ret;
    }
}