package ir.opt;

import ir.instruction.BinaryOp;
import ir.instruction.Instr;
import ir.value.CompUnit;
import ir.value.InitVal;

import java.util.ArrayList;
import java.util.List;

public class InstrSimplify implements Pass {
    List<Instr> workList = new ArrayList<>();
    List<Instr> queueFree = new ArrayList<>();

    @Override
    public void run(CompUnit compUnit) {
        compUnit.forEveryInstr(instr -> {
            if (instr instanceof BinaryOp && instr.getUsers().size() == 1) {
                BinaryOp binaryOp = (BinaryOp) instr;
                if (!(binaryOp.getUsers().get(0).getValue() instanceof BinaryOp)) {
                    return;
                }
                BinaryOp user = (BinaryOp) binaryOp.getUsers().get(0).getValue();

                if (binaryOp.getOpType() == BinaryOp.OpType.Add && user.getOpType() == BinaryOp.OpType.Add) {
                    if (binaryOp.getRight() instanceof InitVal && user.getRight() instanceof InitVal) {
                        user.replaceUseWith(binaryOp, binaryOp.getLeft());
                        user.replaceUseWith(user.getRight(),
                                InitVal.buildInitVal(((InitVal) binaryOp.getRight()).getValue() + ((InitVal) user.getRight()).getValue()));
                        //binaryOp.removeMeFromAllMyUses();
                        queueFree.add(binaryOp);
                    }
                }

                if (binaryOp.getOpType() == BinaryOp.OpType.Sub && user.getOpType() == BinaryOp.OpType.Sub) {
                    if (binaryOp.getRight() instanceof InitVal && user.getRight() instanceof InitVal) {
                        user.replaceUseWith(binaryOp, binaryOp.getLeft());
                        user.replaceUseWith(user.getRight(),
                                InitVal.buildInitVal(((InitVal) binaryOp.getRight()).getValue() + ((InitVal) user.getRight()).getValue()));
                        //binaryOp.removeMeFromAllMyUses();
                        queueFree.add(binaryOp);
                    }
                }
            }
        });
        queueFree.forEach(instr -> {
            instr.removeMeFromAllMyUses();
            instr.getNode().removeMe();
        });
    }

}
