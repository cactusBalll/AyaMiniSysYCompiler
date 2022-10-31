package backend;

import backend.instr.Label;
import backend.instr.MCData;
import backend.instr.MCInstr;
import util.MyList;
import util.MyNode;

public class MCUnit {
    public MyList<MCInstr> prelude = new MyList<>(); //在main之前执行，如准备栈环境
    public MyList<MCData> data = new MyList<>();
    public MyList<MCFunction> list = new MyList<>();
    public MCBlock endProg = new MCBlock();

    public MCUnit() {
        endProg.label = Label.getLabel(endProg);
    }

    public MCData getDataByName(String name) {
        for (MyNode<MCData> d :
                data) {
            if (d.getValue().label.name.equals(name)) {
                return d.getValue();
            }
        }
        assert false;
        return null;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (data.getSize() > 0) {
            sb.append(".data\n");
            for (MyNode<MCData> d :
                    data) {
                sb.append(d.getValue().toString()).append('\n');
            }
            sb.append(".text\n");
        }
        for (MyNode<MCInstr> instrNode :
                prelude) {
            sb.append('\t').append(instrNode.getValue()).append('\n');
        }
        for (MyNode<MCFunction> blockNode :
                list) {
            MCFunction block = blockNode.getValue();
            sb.append(block.toString());
        }
        sb.append(endProg).append('\n');
        return sb.toString();
    }
}
