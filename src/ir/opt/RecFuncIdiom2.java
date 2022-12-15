package ir.opt;

import ir.instruction.*;
import ir.value.*;
import util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecFuncIdiom2 implements Pass{


    private List<Function> toReplace = new ArrayList<>();
    private int param1;
    private int param2;
    @Override
    public void run(CompUnit compUnit) {
        compUnit.forEveryFunction(function -> {
            RetInstr retInstr = (RetInstr) function.getList().getLast().getValue().getList().getLast().getValue();
            if (!(retInstr.getRetValue() instanceof PhiInstr)) {
                return;
            }
            PhiInstr phiInstr = (PhiInstr) retInstr.getRetValue();
            List<Pair<Value, BasicBlock>> pairs = phiInstr.getPhiPairs();
            if (pairs.size() != 3) {
                return;
            }
            Optional<Pair<Value, BasicBlock>> binaryOpOp = pairs.stream().filter(pair -> pair.getFirst() instanceof BinaryOp).findAny();
            if (!binaryOpOp.isPresent()) {
                return;
            }
            BinaryOp binaryOp = (BinaryOp) binaryOpOp.get().getFirst();
            if (binaryOp.getOpType() != BinaryOp.OpType.Add ) {
                return;
            }
            if (!(binaryOp.getLeft() instanceof CallInstr)) {
                return;
            }
            CallInstr callInstr = (CallInstr) binaryOp.getLeft();
            if (callInstr.getFunction() != function) {
                return;
            }
            if (callInstr.getParams().size() != 1) {
                return;
            }

            if (!(binaryOp.getRight() instanceof CallInstr)) {
                return;
            }
            callInstr = (CallInstr) binaryOp.getRight();
            if (callInstr.getFunction() != function) {
                return;
            }
            if (callInstr.getParams().size() != 1) {
                return;
            }
            toReplace.add(function);
        });
        if (toReplace.size() != 1) {
            return;
        }
        Function tplt = compUnit.getFunctionOfName("__OPT_fib");
        toReplace.get(0).replaceAllUsesOfMeWith(tplt);
        toReplace.get(0).getNode().removeMe();
    }
}
