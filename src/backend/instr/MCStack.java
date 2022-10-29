package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;
import exceptions.BackEndErr;

import java.util.HashSet;
import java.util.Set;

/**
 * 栈操作 翻译为 addu $sp, offset
 */
public class MCStack extends MCInstr{
    public int offset;

    public MCStack(int offset) {
        this.offset = offset;
    }

    @Override
    public Set<Reg> getDef() {
        Set<Reg> ret = new HashSet<>();
        try {
            ret.add(PReg.getRegByName("sp"));
            return ret;
        } catch (BackEndErr e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void allocate(Reg vReg, PReg pReg) {

    }

    @Override
    public String toString() {
        try {
            return String.format("addu %s,%s,%d", PReg.getRegByName("sp"),PReg.getRegByName("sp"), offset);
        } catch (BackEndErr e) {
            throw new RuntimeException(e);
        }
    }
}
