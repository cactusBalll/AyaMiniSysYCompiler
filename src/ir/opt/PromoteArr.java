package ir.opt;

import ir.instruction.AllocInstr;
import ir.instruction.Instr;
import ir.instruction.LoadInstr;
import ir.instruction.StoreInstr;
import ir.value.BasicBlock;
import ir.value.CompUnit;
import ir.value.Function;
import ir.value.InitVal;
import ty.IntArrTy;
import util.MyNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PromoteArr implements Pass{
    @Override
    public void run(CompUnit compUnit) {
        compUnit.getGlobalValueList().forEach(allocNode-> {
            AllocInstr allocInstr = allocNode.getValue();
            if (allocInstr.getAllocTy() instanceof IntArrTy) {
                Set<Function> userIn = new HashSet<>();
                allocInstr.getUsers().forEach(userNode -> {
                    userIn.add(inWhichFunc(compUnit, (Instr) userNode.getValue()));
                });
                if (userIn.size() > 1) {
                    return; // user 只在一个函数中
                }
                if (allocInstr.getUsers().stream()
                        .allMatch(userMyNode ->
                                (userMyNode.getValue() instanceof LoadInstr &&
                                        ((LoadInstr) userMyNode.getValue()).getIndexes() instanceof InitVal)||
                                        userMyNode.getValue() instanceof StoreInstr &&
                                                ((StoreInstr) userMyNode.getValue()).getIndex() instanceof InitVal)) {
                    Function inFunc = userIn.iterator().next();
                    Map<Integer, AllocInstr> allocMap = new HashMap<>();

                }
            }
        });
    }

    private Function inWhichFunc(CompUnit compUnit, Instr instr) {
        for (MyNode<Function> funcNode :
                compUnit.getList()) {
            for (MyNode<BasicBlock> bbNode : funcNode.getValue().getList()) {
                if (bbNode.getValue() == instr.bbBelongTo) {
                    return funcNode.getValue();
                }
            }
        }
        return null;
    }
}
