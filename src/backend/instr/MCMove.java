package backend.instr;

import backend.regs.Reg;

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
}
