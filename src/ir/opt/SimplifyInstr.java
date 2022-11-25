package ir.opt;

import ir.instruction.Instr;
import ir.instruction.PhiInstr;
import ir.value.BasicBlock;
import ir.value.CompUnit;
import ir.value.Value;
import util.Pair;

import java.util.ArrayList;
import java.util.List;

public class SimplifyInstr implements Pass{
    @Override
    public void run(CompUnit compUnit) {
        List<Instr> toRemove = new ArrayList<>();
        compUnit.forEveryInstr(instr -> {
            if (instr instanceof PhiInstr) {
                PhiInstr phiInstr = (PhiInstr) instr;
                List<Pair<Value, BasicBlock>> pairs = phiInstr.getPhiPairs();
                Value v = pairs.get(0).getFirst();
                for (Pair<Value, BasicBlock> pair :
                        pairs) {
                    if (v != pair.getFirst()) {
                        return;
                    }
                }
                // phi的所有分支值都一样
                phiInstr.replaceAllUsesOfMeWith(v);
                phiInstr.removeMeFromAllMyUses();
                toRemove.add(phiInstr);
            }
        });
        toRemove.forEach(instr -> instr.getNode().removeMe());
    }
}
