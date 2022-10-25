package backend.instr;

import backend.MCBlock;

public class MCJal extends MCInstr{
    public MCBlock target;

    public MCJal(MCBlock target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return "jal" + ' ' + target.label.toString();
    }
}
