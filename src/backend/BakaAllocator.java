package backend;

import backend.instr.MCInstr;
import backend.instr.MCLw;
import backend.instr.MCSw;
import backend.regs.PReg;
import backend.regs.Reg;
import backend.regs.VReg;
import exceptions.BackEndErr;
import util.MyNode;

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
        mcFunction.push.offset = mcFunction.stackSlot * 4;
        mcFunction.pop.offset = mcFunction.stackSlot * 4;
        PReg d = PReg.getRegByName("t0");
        PReg t = PReg.getRegByName("t1");
        PReg s = PReg.getRegByName("t2");
        for (MyNode<MCBlock> bbNode :
                mcFunction.list) {
            MCBlock bb = bbNode.getValue();
            for (MCInstr instr :
                    bb.list.toList()) {
                Set<Reg> uses = instr.getUse();
                int cnt = 0;
                for (Reg r :
                        uses) {
                    if (r instanceof VReg) {
                        MCLw load = new MCLw(
                                PReg.getRegByName("sp"),
                                PReg.getRegById(t.id + cnt),
                                ((VReg) r).id * 4 + mcFunction.stackTop * 4
                        );
                        instr.allocate(r, PReg.getRegById(t.id + cnt));
                        instr.getNode().insertBefore(load.getNode());
                        cnt++;
                    }
                }
                if (instr.getDef().size() == 1) {
                    Reg def = instr.getDef().iterator().next();
                    if (def instanceof VReg) {
                        MCSw store = new MCSw(
                                PReg.getRegByName("sp"),
                                d,
                                ((VReg) def).id * 4 + mcFunction.stackTop * 4
                        );
                        instr.allocate(def, d);
                        instr.getNode().insertAfter(store.getNode());
                    }
                }
            }
        }

    }
}
