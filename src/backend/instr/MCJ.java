package backend.instr;

import backend.MCBlock;

public class MCJ extends MCInstr{
    public MCBlock target;

    public MCJ(MCBlock target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return 'j' + ' ' + target.label.toString();
    }
}
