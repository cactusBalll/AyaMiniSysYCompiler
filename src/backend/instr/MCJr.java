package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;

import java.util.HashSet;
import java.util.Set;

public class MCJr extends MCInstr{
    public Reg s;

    public MCJr(Reg s) {
        this.s = s;
    }

    @Override
    public String toString() {
        return "jr" + ' ' + s.toString();
    }

    @Override
    public Set<Reg> getUse() {
        Set<Reg> ret = new HashSet<>();
        ret.add(s);
        return ret;
    }

    @Override
    public void allocate(Reg vReg, PReg pReg) {
        if (vReg == s) {
            s = pReg;
        }
    }
}
