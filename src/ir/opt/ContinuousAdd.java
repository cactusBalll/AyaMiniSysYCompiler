package ir.opt;

import ir.instruction.BinaryOp;
import ir.instruction.Instr;
import ir.value.CompUnit;
import ir.value.InitVal;
import ir.value.User;
import ir.value.Value;

import java.util.ArrayList;
import java.util.List;

public class ContinuousAdd implements Pass {

    List<Instr> queueFree = new ArrayList<>();

    @Override
    public void run(CompUnit compUnit) {
        compUnit.forEveryBasicBlock(basicBlock -> {
            List<Instr> list = basicBlock.getList().toList();
            for (int i = 0, listSize = list.size(); i < listSize; i++) {
                Instr instr = list.get(i);
                if (instr instanceof BinaryOp && ((BinaryOp) instr).getOpType() == BinaryOp.OpType.Add) {
                    i += simplifyContinuousAdd((BinaryOp) instr);
                }
            }
            queueFree.forEach(User::removeMeFromAllMyUses);
            queueFree.forEach(instr -> instr.getNode().removeMe());
        });
    }

    private int simplifyContinuousAdd(BinaryOp binaryOp) {
        int cnt = 0;
        Value v = binaryOp.getLeft();
        BinaryOp op = binaryOp;
        List<Instr> toReplace = new ArrayList<>();
        while (true) {
            if (op.getOpType() == BinaryOp.OpType.Add &&
                    op.getRight() == binaryOp.getRight()) {
                toReplace.add(op);
                cnt++;
                if (op.getUsers().size() == 1) {
                    User user = op.getUsers().get(0).getValue();
                    if (user instanceof BinaryOp && ((BinaryOp) user).getLeft() == op) {
                        op = (BinaryOp) user;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        if (cnt > 4) {
            Value init = binaryOp.getLeft();
            if (init == binaryOp.getRight()) {
                BinaryOp last = (BinaryOp) toReplace.get(toReplace.size() - 1);
                last.replaceUseWith(last.getRight(), InitVal.buildInitVal(cnt + 1));
                last.replaceUseWith(last.getLeft(), init);

                last.setOpType(BinaryOp.OpType.Mul);
                toReplace.remove(last);
            } else {
                Value fold = binaryOp.getRight();
                binaryOp.replaceUseWith(
                        binaryOp.getRight(),
                        InitVal.buildInitVal(cnt)
                );
                binaryOp.replaceUseWith(
                        init,
                        fold
                );

                binaryOp.setOpType(BinaryOp.OpType.Mul);
                BinaryOp last = (BinaryOp) toReplace.get(toReplace.size() - 1);
                last.replaceUseWith(last.getLeft(), binaryOp);
                last.replaceUseWith(last.getRight(), init);
                toReplace.remove(binaryOp);
                toReplace.remove(last);
                queueFree.addAll(toReplace);
            }
            return cnt - 1;

        }
        return 0;
    }
}
