package backend;

import backend.instr.Label;
import backend.instr.MCStack;
import ir.value.BasicBlock;
import util.MyList;
import util.MyNode;

public class MCFunction {
    public MyList<MCBlock> list = new MyList<>();

    public Label label;
    public int regAllocated = 0; //共分配了多少个虚拟寄存器
    public int stackSlot;

    public int stackTop; // 不包括参数占用的位置的当前栈顶
    public MCBlock headBB; // 函数头部基本块

    public MCStack push;
    public MCStack pop;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append('\n');
        for (MyNode<MCBlock> bbNode:
             list) {
            MCBlock bb = bbNode.getValue();
            sb.append(bb.toString());
        }

        return sb.toString();
    }

    private final MyNode node = new MyNode<>(this);

    public MyNode getNode() {
        return node;
    }
}
