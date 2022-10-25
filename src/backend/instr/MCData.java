package backend.instr;

import util.MyNode;

public class MCData {
    public Label label;
    private final MyNode node = new MyNode<>(this);

    public MyNode getNode() {
        return node;
    }

    public MCData(Label label) {
        this.label = label;
    }
}
