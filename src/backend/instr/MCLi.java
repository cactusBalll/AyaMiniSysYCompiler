package backend.instr;

import backend.regs.Reg;

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
}
