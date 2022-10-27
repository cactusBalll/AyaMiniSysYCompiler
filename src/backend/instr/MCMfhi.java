package backend.instr;

import backend.regs.Reg;

import java.util.HashSet;
import java.util.Set;

public class MCMfhi extends MCInstr{
    public Reg d;

    public MCMfhi(Reg d) {
        this.d = d;
    }

    @Override
    public String toString() {
        return "mfhi " + d.toString();
    }

    @Override
    public Set<Reg> getDef() {
        Set<Reg> ret = new HashSet<>();
        ret.add(d);
        return  ret;
    }
}
