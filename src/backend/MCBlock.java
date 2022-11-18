package backend;

import backend.instr.Label;
import backend.instr.MCInstr;
import backend.instr.MCStack;
import backend.regs.Reg;
import util.MyList;
import util.MyNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MCBlock {
    public Label label;
    public MyList<MCInstr> list = new MyList<>();

    public List<MCBlock> prec = new ArrayList<>();
    public List<MCBlock> succ = new ArrayList<>();

    public Set<Reg> liveIn = new HashSet<>();
    public Set<Reg> liveOut = new HashSet<>();
    public Set<Reg> def = new HashSet<>();
    public Set<Reg> use = new HashSet<>();

    public int loopDepth;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append('\n');
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
