package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;
import exceptions.BackEndErr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    public List<Reg> getUse() {
        List<Reg> ret = new ArrayList<>();
        ret.add(s);
        return ret;
    }

    @Override
    public void allocate(Reg vReg, Reg pReg) {
        if (vReg == s) {
            s = pReg;
        }
    }
}
