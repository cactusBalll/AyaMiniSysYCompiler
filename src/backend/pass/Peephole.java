package backend.pass;

import backend.MCBlock;
import backend.MCFunction;
import backend.MCUnit;
import backend.instr.MCInstr;
import backend.instr.MCInstrB;
import backend.instr.MCInstrI;
import backend.instr.MCInstrR;
import backend.regs.PReg;
import exceptions.BackEndErr;
import util.MyNode;

import java.util.ArrayList;
import java.util.List;

public class Peephole implements MCPass {
    @Override
    public void run(MCUnit unit) {
        for (MyNode<MCFunction> funcNode :
                unit.list) {
            for (MyNode<MCBlock> bbNode :
                    funcNode.getValue().list) {
                MCBlock bb = bbNode.getValue();
                List<MCInstr> instrList = bb.list.toList();
                List<MCInstr> toRemove = new ArrayList<>();
                // 合并set，branch序列
                for (int i = 0; i < instrList.size() - 1; i++) {
                    if (instrList.get(i) instanceof MCInstrI && instrList.get(i + 1) instanceof MCInstrB) {
                        MCInstrI mcInstrI = (MCInstrI) instrList.get(i);
                        MCInstrB mcInstrB = (MCInstrB) instrList.get(i + 1);
                        if (mcInstrB.isBnez() && mcInstrB.s == mcInstrI.t && mcInstrI.imm == 0) {
                            boolean flag = false;
                            if (mcInstrI.type == MCInstrI.Type.seq) {
                                mcInstrB.type = MCInstrB.Type.beqz;
                                flag = true;
                            }
                            if (mcInstrI.type == MCInstrI.Type.sle) {
                                mcInstrB.type = MCInstrB.Type.blez;
                                flag = true;
                            }
                            if (mcInstrI.type == MCInstrI.Type.slti) {
                                mcInstrB.type = MCInstrB.Type.bltz;
                                flag = true;
                            }
                            if (mcInstrI.type == MCInstrI.Type.sgt) {
                                mcInstrB.type = MCInstrB.Type.bgtz;
                                flag = true;
                            }
                            if (mcInstrI.type == MCInstrI.Type.sge) {
                                mcInstrB.type = MCInstrB.Type.bgez;
                                flag = true;
                            }
                            if (flag) {
                                mcInstrB.s = mcInstrI.s;
                                toRemove.add(mcInstrI);
                            }
                        }
                    }
                    if (instrList.get(i) instanceof MCInstrR && instrList.get(i + 1) instanceof MCInstrB) {
                        MCInstrR mcInstrR = (MCInstrR) instrList.get(i);
                        MCInstrB mcInstrB = (MCInstrB) instrList.get(i + 1);

                        if (mcInstrB.isBnez() && mcInstrB.s == mcInstrR.d) {
                            boolean flag = false;
                            if (mcInstrR.type == MCInstrR.Type.seq) {
                                mcInstrB.type = MCInstrB.Type.beq;
                                mcInstrB.s = mcInstrR.s;
                                mcInstrB.t = mcInstrR.t;
                                flag = true;
                            }

                            if (mcInstrR.type == MCInstrR.Type.sne) {
                                mcInstrB.type = MCInstrB.Type.bne;
                                mcInstrB.s = mcInstrR.s;
                                mcInstrB.t = mcInstrR.t;
                                flag = true;
                            }

                            if (flag) {
                                toRemove.add(mcInstrR);
                            }

                        }
                    }
                }
                toRemove.forEach(mcInstr -> mcInstr.getNode().removeMe());
            }
        }
    }
}
