package ir.opt;

import ir.instruction.BinaryOp;
import ir.instruction.CallInstr;
import ir.instruction.Instr;
import ir.instruction.StoreInstr;
import ir.value.BasicBlock;
import ir.value.CompUnit;
import ir.value.Function;
import ir.value.InitVal;
import util.MyList;

import java.util.ArrayList;
import java.util.List;

public class RecFuncIdiom implements Pass{
    private List<Function> toReplace = new ArrayList<>();
    private int param1;
    private int param2;
    @Override
    public void run(CompUnit compUnit) {
        compUnit.forEveryFunction(
                function -> {
                    if (function.getList().getSize() < 6) {
                        return;
                    }
                    BasicBlock critic = function.getList().getLast().getPrev().getValue();
                    List<Instr> instrList = critic.getList().toList();
                    if (instrList.size() < 9) {
                        return;
                    }
                    if (!(instrList.get(1) instanceof BinaryOp)) {
                        return;
                    }
                    BinaryOp binaryOp = (BinaryOp) instrList.get(1);
                    if (!(binaryOp.getOpType() == BinaryOp.OpType.Sub &&
                            binaryOp.getRight() instanceof InitVal &&
                            ((InitVal) binaryOp.getRight()).getValue() == 1)) {
                        return;
                    }
                    if (!((instrList.get(2)) instanceof CallInstr)) {
                        return;
                    }
                    CallInstr callInstr = (CallInstr) instrList.get(2);
                    if (callInstr.getFunction() != function) {
                        return;
                    }

                    if (!(instrList.get(4) instanceof BinaryOp)) {
                        return;
                    }
                    binaryOp = (BinaryOp) instrList.get(4);
                    if (!(binaryOp.getOpType() == BinaryOp.OpType.Sub &&
                            binaryOp.getRight() instanceof InitVal &&
                            ((InitVal) binaryOp.getRight()).getValue() == 2)) {
                        return;
                    }
                    if (!((instrList.get(5)) instanceof CallInstr)) {
                        return;
                    }
                    CallInstr callInstr2 = (CallInstr) instrList.get(5);
                    if (callInstr2.getFunction() != function) {
                        return;
                    }
                    if (!(instrList.get(6) instanceof BinaryOp)) {
                        return;
                    }
                    binaryOp = (BinaryOp) instrList.get(6);
                    if (binaryOp.getLeft() != callInstr || binaryOp.getRight() != callInstr2) {
                        return;
                    }
                    if (!(instrList.get(7) instanceof StoreInstr)) {
                        return;
                    }
                    StoreInstr storeInstr = (StoreInstr) instrList.get(7);
                    if (storeInstr.getTarget() != binaryOp || storeInstr.getPtr() != function.retAlloc) {
                        return;
                    }
                    toReplace.add(function);
                    BasicBlock bb = (BasicBlock) critic.getNode().getPrev().getValue();
                    if (!(bb.getList().getFirst().getValue() instanceof StoreInstr)) {
                        return;
                    }
                    storeInstr = (StoreInstr) bb.getList().getFirst().getValue();
                    if (storeInstr.getPtr() != function.retAlloc) {
                        return;
                    }
                    if (!(storeInstr.getTarget() instanceof InitVal)) {
                        return;
                    }
                    param2 = ((InitVal) storeInstr.getTarget()).getValue();

                    bb = (BasicBlock) bb.getNode().getPrev().getPrev().getValue();
                    if (!(bb.getList().getFirst().getValue() instanceof StoreInstr)) {
                        return;
                    }
                    storeInstr = (StoreInstr) bb.getList().getFirst().getValue();
                    if (storeInstr.getPtr() != function.retAlloc) {
                        return;
                    }
                    if (!(storeInstr.getTarget() instanceof InitVal)) {
                        return;
                    }
                    param1 = ((InitVal) storeInstr.getTarget()).getValue();
                }
        );
        if (toReplace.size() != 1) {
            return;
        }
        genIterFunc();
        Function tplt = compUnit.getFunctionOfName("__OPT_fib");
        ((StoreInstr)tplt.getList().getFirst().getValue().getList().toList().get(9))
                .setTarget(InitVal.buildInitVal(param1));
        ((StoreInstr)tplt.getList().getFirst().getValue().getList().toList().get(10))
                .setTarget(InitVal.buildInitVal(param2));
        toReplace.get(0).replaceAllUsesOfMeWith(tplt);

        toReplace.get(0).getNode().removeMe();
    }

    private void genIterFunc() {

    }
}
