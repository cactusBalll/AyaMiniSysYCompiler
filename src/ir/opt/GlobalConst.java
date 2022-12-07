package ir.opt;

import ir.instruction.AllocInstr;
import ir.instruction.Instr;
import ir.instruction.LoadInstr;
import ir.value.CompUnit;
import ir.value.InitVal;
import ir.value.User;
import ty.IntTy;

import java.util.ArrayList;
import java.util.List;

public class GlobalConst implements Pass {
    List<AllocInstr> workList = new ArrayList<>();

    @Override
    public void run(CompUnit compUnit) {
        compUnit.getGlobalValueList().forEach(allocNode -> {
            if (allocNode.getValue().getAllocTy() instanceof IntTy && allocNode.getValue().getUsers().stream().allMatch(
                    userNode -> userNode.getValue() instanceof LoadInstr)) {
                workList.add(allocNode.getValue());
            }
        }); // 标记只被load过的全局int变量
        List<Instr> queueFree = new ArrayList<>();
        workList.forEach(allocInstr -> {
            allocInstr.getUsers().forEach(userNode -> {
                LoadInstr loadInstr = (LoadInstr) userNode.getValue();
                loadInstr.replaceAllUsesOfMeWith(InitVal.buildInitVal(allocInstr.getInitVal().getValue()));
                //loadInstr.removeMeFromAllMyUses();
                queueFree.add(loadInstr);
            });
        });
        queueFree.forEach(instr -> {
            instr.removeMeFromAllMyUses();
            instr.getNode().removeMe();
        });
    }
}
