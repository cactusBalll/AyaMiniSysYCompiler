package ir.value;

import ir.instruction.AllocInstr;
import ir.instruction.Instr;
import util.MyList;
import util.MyNode;

import java.util.function.Consumer;

public class CompUnit {

    private final MyList<AllocInstr> globalValueList = new MyList<>();
    private final MyList<Function> list = new MyList<>();

    public MyList<Function> getList() {
        return list;
    }

    public MyList<AllocInstr> getGlobalValueList() {
        return globalValueList;
    }

    public void addGlobalValue(AllocInstr allocInstr) {
        globalValueList.add(allocInstr.getNode());
    }

    public void setValueName() {
        int cnt = 0;
        for (MyNode<Function> funcNode :
                list) {
            cnt = 0;
            Function func = funcNode.getValue();
            for (Param p :
                    func.getParams()) {
                p.setName("%"+cnt);
                cnt++;
            }
            for (MyNode<BasicBlock> bbNode :
                    func.getList()) {
                BasicBlock bb = bbNode.getValue();
                bb.setName("%"+cnt);
                cnt++;
                for (MyNode<Instr> instrNode :
                        bb.getList()) {
                    Instr instr = instrNode.getValue();
                    instr.setName("%"+cnt);
                    cnt++;
                }
            }
        }
    }
    public void fullMaintain() {
        maintainUser();
        maintainBBelong();
    }
    public void maintainUser() {
        forEveryInstr(instr -> instr.users.clear());
        for (MyNode<Function> funcNode :
                list) {
            Function func = funcNode.getValue();
            for (MyNode<BasicBlock> bbNode :
                    func.getList()) {
                BasicBlock bb = bbNode.getValue();
                for (MyNode<Instr> instrNode :
                        bb.getList()) {
                    Instr instr = instrNode.getValue();
                    for (MyNode<Value> useNode:
                        instr.getUses()){
                        Value use = useNode.getValue();
                        use.users.add(instr.getNode());
                    }
                }
            }
        }
    }

    public void maintainBBelong() {
        forEveryBasicBlock(bb -> {
            for (MyNode<Instr> instrNode :
                    bb.getList()) {
                instrNode.getValue().bbBelongTo = bb;
            }
        });
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (MyNode<AllocInstr> allocNode :
                globalValueList) {
            sb.append(allocNode.getValue().toString()).append('\n');
        }
        sb.append('\n');
        for (MyNode<Function> funcNode :
                list) {
            sb.append(funcNode.getValue().toString()).append("\n\n");
        }
        return sb.toString();
    }

    public void forEveryFunction(java.util.function.Consumer<Function> f) {
        for (MyNode<Function> funcNode :
                list) {
            f.accept(funcNode.getValue());
        }
    }

    public void forEveryBasicBlock(Consumer<BasicBlock> f) {
        for (MyNode<Function> funcNode :
                list) {
            Function func = funcNode.getValue();
            for (MyNode<BasicBlock> bbNode:
                    func.getList()) {
                f.accept(bbNode.getValue());
            }
        }
    }

    public void forEveryInstr(Consumer<Instr> f) {
        for (MyNode<Function> funcNode :
                list) {
            Function func = funcNode.getValue();
            for (MyNode<BasicBlock> bbNode:
                    func.getList()) {
                BasicBlock bb = bbNode.getValue();
                for (MyNode<Instr> instrNode :
                        bb.getList()) {
                    f.accept(instrNode.getValue());
                }
            }
        }
    }
}
