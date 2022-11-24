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
                List<MCInstr> brOrJmps = new ArrayList<>();
                List<MCBlock> targets = new ArrayList<>();
                for (MyNode<MCInstr> instrNode :
                        bb.list) {
                    MCInstr instr = instrNode.getValue();
                    if (instr instanceof MCJ) {
                        brOrJmps.add(instr);
                        targets.add(((MCJ) instr).target);
                    }
                    if (instr instanceof MCInstrB) {
                        brOrJmps.add(instr);
                        targets.add(((MCInstrB) instr).target);
                    }
                }
                for (int i = 0; i < targets.size(); i++) {
                    MCBlock target = targets.get(i);
                    if (target.list.getSize() == 1 && target.list.getLast().getValue() instanceof MCJ) {
                        MCJ indirectJ = (MCJ) target.list.getLast().getValue();

                        ((MCJ) brOrJmps.get(i)).target = indirectJ.target;
                        target.prec.remove(bb);
                    }
                }


            }
        }
    }
}
