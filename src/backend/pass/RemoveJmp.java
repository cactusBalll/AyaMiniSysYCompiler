package backend.pass;

import backend.MCBlock;
import backend.MCFunction;
import backend.MCUnit;
import backend.instr.MCInstr;
import backend.instr.MCJ;
import ir.instruction.Instr;
import ir.instruction.JmpInstr;
import util.MyNode;

public class RemoveJmp implements MCPass{
    @Override
    public void run(MCUnit unit) {
        for (MyNode<MCFunction> funcNode :
                unit.list) {
            MCFunction func = funcNode.getValue();
            for (MyNode<MCBlock> bbNode :
                    func.list) {
                MCBlock bb = bbNode.getValue();
                MCInstr lastInstr = bb.list.getLast().getValue();
                if (lastInstr instanceof MCJ) {
                    MCBlock target = ((MCJ) lastInstr).target;
                    if (bb.getNode().getNext() == target.getNode()) {
                        // 跳转到后继块可以删掉这个j
                        lastInstr.getNode().removeMe();
                    }
                }
            }
        }
    }
}
