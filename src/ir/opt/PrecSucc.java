package ir.opt;

import ir.instruction.BrInstr;
import ir.instruction.Instr;
import ir.instruction.JmpInstr;
import ir.value.BasicBlock;
import ir.value.CompUnit;

public class PrecSucc implements Pass{
    @Override
    public void run(CompUnit compUnit) {
        compUnit.forEveryBasicBlock(BasicBlock::clearBBInfo);
        compUnit.forEveryBasicBlock(BasicBlock::clearPrecSucc);
        compUnit.forEveryBasicBlock(bb-> {
            Instr brInstr = bb.getList().getLast().getValue();
            if (brInstr instanceof BrInstr) {
                bb.succ.add(((BrInstr) brInstr).getBr0());
                bb.succ.add(((BrInstr) brInstr).getBr1());
                ((BrInstr) brInstr).getBr0().prec.add(bb);
                ((BrInstr) brInstr).getBr1().prec.add(bb);
            }
            if (brInstr instanceof JmpInstr) {
                bb.succ.add(((JmpInstr) brInstr).getTarget());
                ((JmpInstr) brInstr).getTarget().prec.add(bb);
            }
        });
    }
}
