package ir.opt;

import ir.instruction.*;
import ir.value.BasicBlock;
import ir.value.CompUnit;
import ir.value.Function;
import util.MyNode;

/**
 * 标记函数：pure和recursive
 */
public class MarkFunc implements Pass{
    @Override
    public void run(CompUnit compUnit) {
        compUnit.forEveryFunction(function -> {
            function.setPure(true);
            if (!isPure(function)) {
                function.setPure(false);
            }
            checkRecursive(function);
        });
    }
    private void checkRecursive(Function function) {
        for (MyNode<BasicBlock> bbNode :
                function.getList()) {
            BasicBlock bb = bbNode.getValue();
            for (MyNode<Instr> instrNode :
                    bb.getList()) {
                Instr instr = instrNode.getValue();
                if (instr instanceof CallInstr) {
                    CallInstr callInstr = (CallInstr)instr;
                    if (callInstr.getFunction() == function) {
                        function.setRecursive(true);
                        return;
                    }
                }
            }
        }
    }
    private boolean isPure(Function function) {
        for (MyNode<BasicBlock> bbNode :
                function.getList()) {
            BasicBlock bb = bbNode.getValue();
            for (MyNode<Instr> instrNode :
                    bb.getList()) {
                Instr instr = instrNode.getValue();
                if (instr instanceof LoadInstr || instr instanceof StoreInstr ||
                        instr instanceof BuiltinCallInstr || instr instanceof ArrView) {
                    return false;
                }
                if (instr instanceof CallInstr) {
                    CallInstr callInstr = (CallInstr)instr;
                    if (!callInstr.getFunction().isPure()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
