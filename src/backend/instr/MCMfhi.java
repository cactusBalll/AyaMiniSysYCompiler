package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    public List<Reg> getDef() {
        List<Reg> ret = new ArrayList<>();
        ret.add(d);
        return  ret;
    }

    @Override
    public void allocate(Reg vReg, Reg pReg) {
        if (d == vReg) {
            d = pReg;
        }
    }
}
