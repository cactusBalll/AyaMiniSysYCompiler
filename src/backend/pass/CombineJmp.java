package backend.pass;

import backend.MCBlock;
import backend.MCFunction;
import backend.MCUnit;
import backend.instr.MCInstr;
import backend.instr.MCInstrB;
import backend.instr.MCJ;
import ir.instruction.Instr;
import util.MyNode;

import java.util.ArrayList;
import java.util.List;

public class CombineJmp implements MCPass{
    @Override
    public void run(MCUnit unit) {
        for (MyNode<MCFunction> funcNode :
                unit.list) {
            MCFunction func = funcNode.getValue();
            for (MyNode<MCBlock> bbNode :
                    func.list) {
                MCBlock bb = bbNode.getValue();
                for (MyNode<MCInstr> instrNode :
                        bb.list) {
                    MCInstr instr = instrNode.getValue();
                    if (instr instanceof MCJ) {
                        MCJ mcj = (MCJ) instr;
                        mcj.target = propTargets(mcj.target);
                    }
                    if (instr instanceof MCInstrB) {
                        MCInstrB mcInstrB = (MCInstrB) instr;
                        mcInstrB.target = propTargets(mcInstrB.target);
                    }
                }
            }
        }
    }
    private MCBlock propTargets(MCBlock mcBlock) {
        MCBlock ret = mcBlock;
        MCBlock last;
        do {
            last = ret;
            ret = propTarget(ret);
        } while (last != ret);
        return ret;
    }
    private MCBlock propTarget(MCBlock mcBlock) {
        if (mcBlock.list.getSize() == 1) {
            MCInstr instr = mcBlock.list.getFirst().getValue();
            if (instr instanceof MCJ) {
                return ((MCJ) instr).target;
            }
        }
        return mcBlock;
    }
}
