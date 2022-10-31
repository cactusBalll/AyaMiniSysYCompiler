package ir.opt;

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
                    binaryOp.getUsers().forEach(vNode-> {
                        Value v = vNode.getValue();
                        if (v instanceof Instr) {
                            instrWorkList.add((Instr) v);
                        }
                    });
                    binaryOp.replaceAllUsesOfMeWith(InitVal.buildInitVal(t));
                    binaryOp.getNode().removeMe();
                }
            }
        }
    }

    private boolean hasSideEffect(Instr instr) {
        return !(instr instanceof ArrView || instr instanceof BinaryOp || instr instanceof PhiInstr);
    }
    @Override
    public void run(CompUnit compUnit) {
        addAllInstr(compUnit);
        simpleDCE();
        addAllInstr(compUnit);
        simpleCP();
    }
}
