package backend.instr;

import backend.MCBlock;
import backend.regs.PReg;
import backend.regs.Reg;
import exceptions.BackEndErr;

import java.util.HashSet;
import java.util.Set;

public class MCJal extends MCInstr{
    public MCBlock target;

    public MCJal(MCBlock target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return "jal" + ' ' + target.label.name;
    }

    @Override
    public Set<Reg> getDef() {
        Set<Reg> ret = new HashSet<>();
        try {
            ret.add(PReg.getRegByName("ra"));
        } catch (BackEndErr e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Override
    public void allocate(Reg vReg, PReg pReg) {

    }
}
