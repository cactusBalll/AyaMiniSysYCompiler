package backend.pass;

import backend.MCBlock;
import backend.MCFunction;
import backend.MCUnit;
import backend.instr.MCInstr;
import backend.regs.Reg;
import util.MyNode;

import java.util.HashSet;
import java.util.Set;

/**
 * 计算liveIn，liveOut信息
 */
public class CalcuLiveInfo implements MCPass{
    @Override
    public void run(MCUnit unit) {
        for (MyNode<MCFunction> funcNode :
                unit.list) {
            MCFunction func = funcNode.getValue();
            runOnFunction(func);
        }
    }

    public void runOnFunction(MCFunction function) {
        // 设置每个基本块的def，use列表
        for (MyNode<MCBlock> bbNode :
                function.list) {
            MCBlock bb = bbNode.getValue();
            bb.def.clear();
            bb.use.clear();
            bb.liveIn.clear();
            bb.liveOut.clear();
            bb.list.forEach(
                    instrNode -> {
                        MCInstr instr = instrNode.getValue();
                        instr.getUse().forEach(reg -> {
                            if (!bb.def.contains(reg)) {
                                bb.use.add(reg);
                            }
                        });
                        instr.getDef().forEach(reg -> {
                            if (!bb.use.contains(reg)) {
                                bb.def.add(reg);
                            }
                        });
                    }
            );
        }
        changed = true;
        while (changed) {
            changed = false;
            visited.clear();
            walkMCBlock(function.headBB);
        }
    }
    private boolean changed;
    private Set<MCBlock> visited = new HashSet<>();
    private void walkMCBlock(MCBlock nw) {
        if (visited.contains(nw)) {
            return;
        }
        visited.add(nw);
        nw.succ.forEach(this::walkMCBlock);

        Set<Reg> old = nw.liveOut;
        nw.liveOut = new HashSet<>();
        nw.succ.forEach(bb -> nw.liveOut.addAll(bb.liveIn));
        if (!old.equals(nw.liveOut)) {
            changed = true;
        }

        old = nw.liveIn;
        nw.liveIn = new HashSet<>();
        nw.liveIn.addAll(nw.use);
        Set<Reg> t = new HashSet<>();
        // t = liveOut - Def
        for (Reg r :
                nw.liveOut) {
            if (!nw.def.contains(r)) {
                t.add(r);
            }
        }
        nw.liveIn.addAll(t);
        if (!old.equals(nw.liveIn)) {
            changed = true;
        }
    }
}
