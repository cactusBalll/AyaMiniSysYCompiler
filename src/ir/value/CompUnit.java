package ir.value;

import ir.instruction.AllocInstr;
import ir.instruction.Instr;
import util.MyList;
import util.MyNode;

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

    public void maintainUser() {
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
}
