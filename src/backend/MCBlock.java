package backend;

import backend.instr.Label;
import backend.instr.MCInstr;
import util.MyList;
import util.MyNode;

import java.util.ArrayList;
import java.util.List;

public class MCBlock {
    public Label label;
    public MyList<MCInstr> list;

    public List<MCBlock> prec = new ArrayList<>();
    public List<MCBlock> succ = new ArrayList<>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(':');
        for (MyNode<MCInstr> instrNode :
                list) {
            MCInstr instr = instrNode.getValue();
            sb.append('\t').append(instr.toString()).append('\n');
        }
        return sb.toString();
    }

    private final MyNode node = new MyNode<>(this);

    public MyNode getNode() {
        return node;
    }
}
