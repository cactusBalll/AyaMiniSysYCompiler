package backend;

import backend.instr.MCInstr;
import backend.instr.MCLw;
import backend.instr.MCSw;
import backend.regs.PReg;
import backend.regs.Reg;
import backend.regs.VReg;
import exceptions.BackEndErr;
import util.MyNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *  临时使用的寄存器分配，就是不分配
 *  装作只有三个寄存器。
 */
public class BakaAllocator {
    private MCUnit mcUnit;
    public BakaAllocator(MCUnit mcUnit) {
        this.mcUnit = mcUnit;
    }
    public MCUnit run() throws BackEndErr {
        for (MyNode<MCFunction> funcNode:
                mcUnit.list) {
            MCFunction mcFunction = funcNode.getValue();
            runOnFunction(mcFunction);
        }
        return mcUnit;
    }

    public void runOnFunction(MCFunction mcFunction) throws BackEndErr {
        // 虚拟寄存器全部栈上分配
        mcFunction.stackSlot += mcFunction.regAllocated;
        mcFunction.push.offset = -mcFunction.stackSlot * 4;
        mcFunction.pop.offset = mcFunction.stackSlot * 4;
        PReg d = PReg.getRegByName("t0");
        PReg t = PReg.getRegByName("t1");
        PReg s = PReg.getRegByName("t2");
        for (MyNode<MCBlock> bbNode :
                mcFunction.list) {
            MCBlock bb = bbNode.getValue();
            for (MCInstr instr :
                    bb.list.toList()) {
                if (instr instanceof MCLw && ((MCLw) instr).isLoadArg) {
                    ((MCLw) instr).numOffset += mcFunction.stackSlot * 4;
                }
                Set<Reg> uses = instr.getUse();
                Set<Reg> def = instr.getDef();
                Set<Reg> all = new HashSet<>(def);
                all.addAll(uses);
                Map<VReg, PReg> mapper = new HashMap<>();

                int cnt = 0;
                for (Reg r :
                        all) {
                    if (r instanceof VReg) {
                        mapper.put((VReg) r, PReg.getRegById(t.id + cnt));
                        cnt++;
                    }
                }
                for (Map.Entry<VReg, PReg> p :
                        mapper.entrySet()) {
                    if (uses.contains(p.getKey())) {
                        MCLw load = new MCLw(
                                PReg.getRegByName("sp"),
                                p.getValue(),
                                (p.getKey()).id * 4 + mcFunction.stackTop * 4
                        );
                        instr.getNode().insertBefore(load.getNode());
                    }
                    if (def.contains(p.getKey())) {
                        MCSw store = new MCSw(
                                PReg.getRegByName("sp"),
                                p.getValue(),
                                (p.getKey()).id * 4 + mcFunction.stackTop * 4
                        );
                        instr.getNode().insertAfter(store.getNode());
                    }
                }
                for (Map.Entry<VReg, PReg> p :
                        mapper.entrySet()) {
                    instr.allocate(p.getKey(),p.getValue());
                }
            }
        }

    }
}
