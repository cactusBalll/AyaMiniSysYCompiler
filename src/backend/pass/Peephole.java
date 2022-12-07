package backend.pass;

import backend.MCBlock;
import backend.MCFunction;
import backend.MCUnit;
import backend.instr.*;
import backend.regs.PReg;
import backend.regs.Reg;
import exceptions.BackEndErr;
import ir.value.CompUnit;
import util.MyNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Peephole implements MCPass {
    @Override
    public void run(MCUnit unit) {
        peepHole1(unit);
        peepHoleWithDataFlow(unit);
    }

    private static void peepHole1(MCUnit unit) {
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
                    if (instrList.get(i) instanceof MCLw && instrList.get(i + 1) instanceof MCSw) {
                        MCLw mcLw = (MCLw) instrList.get(i);
                        MCSw mcSw = (MCSw) instrList.get(i + 1);
                        if (mcSw.offset == null && mcLw.offset == null && mcSw.numOffset == mcLw.numOffset &&
                                mcSw.s == mcLw.s && mcSw.t == mcLw.t) {
                            toRemove.add(mcLw);
                            toRemove.add(mcSw);
                        }
                    }
                    if (instrList.get(i) instanceof MCMove && instrList.get(i + 1) instanceof MCInstrR) {
                        MCMove move = (MCMove) instrList.get(i);
                        MCInstrR instrR = (MCInstrR) instrList.get(i + 1);
                        if (move.d == instrR.t && instrR.d == instrR.t) {
                            instrR.t = move.s;
                            toRemove.add(move);
                        }
                    }
                }
                toRemove.forEach(mcInstr -> mcInstr.getNode().removeMe());
            }
        }
    }

    private void peepHoleWithDataFlow(MCUnit unit) {
        for (MyNode<MCFunction> funcNode :
                unit.list) {
            new CalcuLiveInfo().runOnFunctionOpt(funcNode.getValue());
            for (MyNode<MCBlock> bbNode :
                    funcNode.getValue().list) {
                MCBlock bb = bbNode.getValue();
                List<MCInstr> instrList = bb.list.toList();
                List<MCInstr> toRemove = new ArrayList<>();
                Set<Reg> live = new HashSet<>(bb.liveOut);
                for (int i = instrList.size() - 1; i >= 1; i--) {
                    boolean flag = false;
                    if (instrList.get(i) instanceof MCInstrR && isActualMove((MCInstrR) instrList.get(i))) {
                        MCInstrR aMove = (MCInstrR) instrList.get(i);
                        MCInstr op = instrList.get(i - 1);
                        if (!live.contains(aMove.s)) {
                            if (op instanceof MCInstrR) {
                                if (((MCInstrR) op).d == aMove.s) {
                                    ((MCInstrR) op).d = aMove.d;
                                    toRemove.add(aMove);
                                    flag = true;
                                }
                            }
                            if (op instanceof MCInstrI) {
                                if (((MCInstrI) op).t == aMove.s) {
                                    ((MCInstrI) op).t = aMove.d;
                                    toRemove.add(aMove);
                                    flag = true;
                                }
                            }
                            if (op instanceof MCLi) {
                                if (((MCLi) op).t == aMove.s) {
                                    ((MCLi) op).t = aMove.d;
                                    toRemove.add(aMove);
                                    flag = true;
                                }
                            }
                            if (op instanceof MCLw) {
                                if (((MCLw) op).t == aMove.s) {
                                    ((MCLw) op).t = aMove.d;
                                    toRemove.add(aMove);
                                    flag = true;
                                }
                            }
                        }
                    }
                    // 维护活跃信息
                    if (flag) {
                        i--;
                    }
                    instrList.get(i).getDef().forEach(live::remove);
                    live.addAll(instrList.get(i).getUse());
                }
                toRemove.forEach(mcInstr -> mcInstr.getNode().removeMe());
            }
        }
    }

    private boolean isActualMove(MCInstrR mcInstrR) {
        try {
            return mcInstrR.t == PReg.getRegById(0);
        } catch (BackEndErr e) {
            throw new RuntimeException(e);
        }
    }
}
