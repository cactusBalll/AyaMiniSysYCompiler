package backend.instr;

import util.MyNode;

public class MCInstr {
    private final MyNode node = new MyNode<>(this);

    public MyNode getNode() {
        return node;
    }
}
