package backend.instr;

import backend.regs.PReg;
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
        return String.format("la %s,%s",s,target.name);
    }

    @Override
    public Set<Reg> getDef() {
        Set<Reg> ret = new HashSet<>();
        ret.add(s);
        return ret;
    }

    @Override
    public void allocate(Reg vReg, Reg pReg) {
        if (s == vReg) {
            s = pReg;
        }
    }
}
