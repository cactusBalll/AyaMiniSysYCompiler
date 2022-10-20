package ir.opt;

import ir.instruction.Instr;
import ir.value.BasicBlock;
import ir.value.CompUnit;
import util.MyNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * IRGen生成了很多无用块，可以简化
 */
public class SimplifyG implements Pass{
    @Override
    public void run(CompUnit compUnit) {
        compUnit.forEveryFunction(f -> {
            BasicBlock entrance = f.getFirstBB();
            for (BasicBlock bb
                    :f.getList().toList()) {
                if (bb.prec.size() == 0 && bb != entrance) { // 没有前驱，不可达
                    bb.succ.forEach(b->b.prec.remove(bb));
                    bb.getNode().removeMe();
                }
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
                }
            }
        });

    }
}
