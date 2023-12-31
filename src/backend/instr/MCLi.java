package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 伪指令，加载立即数
 */
public class MCLi extends MCInstr{
    public int imm;
    public Reg t;

    public MCLi(int imm, Reg t) {
        this.imm = imm;
        this.t = t;
    }

    @Override
    public String toString() {
        return String.format("li %s,%d",t,imm);
    }

    @Override
    public List<Reg> getDef() {
        List<Reg> ret = new ArrayList<>();
        ret.add(t);
        return ret;
    }

    @Override
    public void allocate(Reg vReg, Reg pReg) {
        if (t == vReg) {
            t = pReg;
        }
    }
}
