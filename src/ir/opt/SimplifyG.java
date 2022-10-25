package ir.opt;

import ir.instruction.BrInstr;
import ir.instruction.Instr;
import ir.instruction.JmpInstr;
import ir.value.BasicBlock;
import ir.value.CompUnit;
import ir.value.Function;
import util.MyNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * IRGen生成了很多无用块，可以简化
 */
public class SimplifyG implements Pass{

    Set<BasicBlock> visited;
    @Override
    public void run(CompUnit compUnit) {
        compUnit.forEveryFunction(f -> {
            BasicBlock entrance = f.getFirstBB();
            mergeBB(f);
            visited = new HashSet<>();
            markBB(entrance);
            for (BasicBlock bb :
                    f.getList().toList()) {
                if (!visited.contains(bb)) {
                    bb.getNode().removeMe();
                    bb.succ.forEach(succBB->succBB.prec.remove(bb));
                }
            }
            mergeBB(f);
        });

    }

    private static void mergeBB(Function f) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (BasicBlock bb
                    : f.getList().toList()) {
                if (bb.prec.size() == 1 &&
                        bb.prec.get(0).succ.size() == 1) { // 唯一前驱后继
                    BasicBlock precBB = bb.prec.get(0);
                    List<Instr> list = bb.getList().toList();
                    precBB.getList().getLast().getValue().removeMeFromAllMyUses();
                    precBB.getList().getLast().removeMe();
                    for (Instr instr : list) {
                        instr.getNode().removeMe();
                        precBB.getList().add(instr.getNode());
                    }
                    precBB.succ.remove(bb);
                    for (BasicBlock afterBB :
                            bb.succ) {
                        precBB.succ.add(afterBB);
                        afterBB.prec.remove(bb);
                        afterBB.prec.add(precBB);
                    }
                    bb.getNode().removeMe();
                    changed = true;
                }
            }
        }
    }

    private void markBB(BasicBlock bb) {
        if (!visited.contains(bb)) {
            visited.add(bb);
            Instr brInstr = bb.getList().getLast().getValue();
            if (brInstr instanceof BrInstr) {
                markBB(((BrInstr) brInstr).getBr0());
                markBB(((BrInstr) brInstr).getBr1());
            } else if (brInstr instanceof JmpInstr) {
                markBB(((JmpInstr) brInstr).getTarget());
            }
        }
    }
}
