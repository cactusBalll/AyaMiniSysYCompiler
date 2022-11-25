package ir.opt;

import Driver.AyaConfig;
import ir.instruction.*;
import ir.value.CompUnit;
import ir.value.InitVal;
import ir.value.Value;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 如果运算指令两边都是立即数后端会出错
 * 这个优化不能全关
 * 简单常数传播
 * 简单死代码删除
 */
public class SimpleCP implements Pass {


    private final Set<Instr> instrWorkList = new HashSet<>();

    private void addAllInstr(CompUnit compUnit) {
        compUnit.forEveryInstr(instrWorkList::add);
    }

    private void simpleDCE() {
        while (!instrWorkList.isEmpty()) {
            Instr instr = instrWorkList.iterator().next();
            instrWorkList.remove(instr);
            if (instr.getUsers().isEmpty() && !hasSideEffect(instr)) {
                instr.getUses().forEach(vNode -> {
                    Value v = vNode.getValue();
                    if (v instanceof Instr) {
                        instrWorkList.add((Instr) vNode.getValue());
                    }
                });
                instr.removeMeFromAllMyUses();
                instr.getNode().removeMe();
            }
        }
    }

    private void simpleCP() {
        while (!instrWorkList.isEmpty()) {
            Instr instr = instrWorkList.iterator().next();
            instrWorkList.remove(instr);
            if (instr instanceof BinaryOp) {
                BinaryOp binaryOp = (BinaryOp) instr;
                Integer t = binaryOp.evaluate();
                if (t != null) {
                    addUser2WorkList(binaryOp);
                    binaryOp.replaceAllUsesOfMeWith(InitVal.buildInitVal(t));
                    binaryOp.getNode().removeMe();
                } else {
                    if (((BinaryOp) instr).getLeft() instanceof InitVal) {
                        int val = ((InitVal) ((BinaryOp) instr).getLeft()).getValue();
                        if (val == 0) {
                            if (binaryOp.getOpType() == BinaryOp.OpType.Add) {
                                addUser2WorkList(binaryOp);
                                binaryOp.replaceAllUsesOfMeWith(binaryOp.getRight());
                                binaryOp.removeMeFromAllMyUses();
                                binaryOp.getNode().removeMe();
                            }
                            if (binaryOp.getOpType() == BinaryOp.OpType.Mul ||
                                    binaryOp.getOpType() == BinaryOp.OpType.Div) {
                                addUser2WorkList(binaryOp);
                                binaryOp.replaceAllUsesOfMeWith(InitVal.buildInitVal(0));
                                binaryOp.removeMeFromAllMyUses();
                                binaryOp.getNode().removeMe();
                            }
                        }
                        if (val == 1) {
                            if (binaryOp.getOpType() == BinaryOp.OpType.Mul) {
                                addUser2WorkList(binaryOp);
                                binaryOp.replaceAllUsesOfMeWith(binaryOp.getRight());
                                binaryOp.removeMeFromAllMyUses();
                                binaryOp.getNode().removeMe();
                            }
                        }
                        if (val == -1) {
                            if (binaryOp.getOpType() == BinaryOp.OpType.Mul) {
                                Instr instr1 =
                                        new BinaryOp(
                                                BinaryOp.OpType.Sub,
                                                InitVal.buildInitVal(0),
                                                binaryOp.getRight());
                                instr1.bbBelongTo = binaryOp.bbBelongTo;
                                addUser2WorkList(binaryOp);
                                binaryOp.replaceAllUsesOfMeWith(instr1);
                                binaryOp.removeMeFromAllMyUses();
                                binaryOp.getNode().insertBefore(instr1.getNode());
                                binaryOp.getNode().removeMe();
                            }
                        }
                    } else if (((BinaryOp) instr).getRight() instanceof InitVal) {
                        int val = ((InitVal) ((BinaryOp) instr).getRight()).getValue();
                        if (val == 0) {
                            if (binaryOp.getOpType() == BinaryOp.OpType.Add) {
                                addUser2WorkList(binaryOp);
                                binaryOp.replaceAllUsesOfMeWith(binaryOp.getLeft());
                                binaryOp.removeMeFromAllMyUses();
                                binaryOp.getNode().removeMe();
                            }
                            if (binaryOp.getOpType() == BinaryOp.OpType.Mul) {
                                addUser2WorkList(binaryOp);
                                binaryOp.replaceAllUsesOfMeWith(InitVal.buildInitVal(0));
                                binaryOp.removeMeFromAllMyUses();
                                binaryOp.getNode().removeMe();
                            }
                            if (binaryOp.getOpType() == BinaryOp.OpType.Div) {
                                throw new RuntimeException(); // div 0
                            }
                        }
                        if (val == 1) {
                            if (binaryOp.getOpType() == BinaryOp.OpType.Mul ||
                                    binaryOp.getOpType() == BinaryOp.OpType.Div) {
                                addUser2WorkList(binaryOp);
                                binaryOp.replaceAllUsesOfMeWith(binaryOp.getLeft());
                                binaryOp.removeMeFromAllMyUses();
                                binaryOp.getNode().removeMe();
                            }

                        }
                        if (val == -1) {
                            if (binaryOp.getOpType() == BinaryOp.OpType.Mul ||
                                    binaryOp.getOpType() == BinaryOp.OpType.Div) {
                                Instr instr1 =
                                        new BinaryOp(
                                                BinaryOp.OpType.Sub,
                                                InitVal.buildInitVal(0),
                                                binaryOp.getLeft());
                                instr1.bbBelongTo = binaryOp.bbBelongTo;
                                addUser2WorkList(binaryOp);
                                binaryOp.replaceAllUsesOfMeWith(instr1);
                                binaryOp.removeMeFromAllMyUses();
                                binaryOp.getNode().insertBefore(instr1.getNode());
                                binaryOp.getNode().removeMe();
                            }
                        }
                    }
                }
            }
        }
    }

    private void addUser2WorkList(BinaryOp binaryOp) {
        binaryOp.getUsers().forEach(vNode -> {
            Value v = vNode.getValue();
            if (v instanceof Instr) {
                instrWorkList.add((Instr) v);
            }
        });
    }


    private boolean hasSideEffect(Instr instr) {
        /*if (instr instanceof CallInstr && ((CallInstr) instr).getFunction().isPure()) {
            System.out.println(String.format("calling pure %s", ((CallInstr) instr).getFunction().getName()));
        }*/
        return !(instr instanceof ArrView || instr instanceof BinaryOp || instr instanceof PhiInstr
                || instr instanceof LoadInstr
                || (instr instanceof CallInstr && ((CallInstr) instr).getFunction().isPure())); // 调用纯函数没有副作用
    }

    @Override
    public void run(CompUnit compUnit) {
        if (AyaConfig.OPT) {
            addAllInstr(compUnit);
            simpleDCE();
        }
        addAllInstr(compUnit);
        simpleCP();
    }
}
