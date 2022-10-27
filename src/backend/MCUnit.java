package backend;

import backend.instr.MCData;
import backend.instr.MCInstr;
import util.MyList;
import util.MyNode;

public class MCUnit {
    public MyList<MCData> data = new MyList<>();
    public MyList<MCFunction> list = new MyList<>();

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
        for (MyNode<MCFunction> blockNode :
                list) {
            MCFunction block = blockNode.getValue();
            sb.append(block.toString());
        }
        return sb.toString();
    }
}
