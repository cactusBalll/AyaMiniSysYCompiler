package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 这是一条伪指令，等价于addu *,$0,*，
 * 移动在寄存器分配和phi消除有特殊的意义
 */
public class MCMove extends MCInstr{
    public Reg d;
    public Reg s;

    public MCMove(Reg d, Reg s) {
        this.d = d;
        this.s = s;
    }

    @Override
    public String toString() {
        return String.format("move %s,%s",d,s);
    }

    @Override
    public List<Reg> getUse() {
        List<Reg> ret = new ArrayList<>();
        ret.add(s);
        return ret;
    }

    @Override
    public List<Reg> getDef() {
        List<Reg> ret = new ArrayList<>();
        ret.add(d);
        return ret;
    }

    @Override
    public void allocate(Reg vReg, Reg pReg) {
        if (vReg == s) {
            s = pReg;
        }
        if (vReg == d) {
            d = pReg;
        }
    }
}
