package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MCMflo extends MCInstr{
    public Reg d;

    public MCMflo(Reg d) {
        this.d = d;
    }

    @Override
    public String toString() {
        return "mflo " + d.toString();
    }

    @Override
    public List<Reg> getDef() {
        List<Reg> ret = new ArrayList<>();
        ret.add(d);
        return  ret;
    }

    @Override
    public void allocate(Reg vReg, Reg pReg) {
        if (vReg == d) {
            d = pReg;
        }
    }
}
