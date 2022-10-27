package backend;

import backend.instr.Label;
import ir.value.BasicBlock;
import util.MyList;
import util.MyNode;

public class MCFunction {
    public MyList<MCBlock> list = new MyList<>();
    public int regAllocated = 0; //共分配了多少个虚拟寄存器
    public int stackSlot;
    public MCBlock headBB; // 函数头部基本块

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

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
