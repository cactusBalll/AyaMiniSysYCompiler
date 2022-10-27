package backend.instr;

import backend.MCBlock;
import backend.regs.Reg;

import java.util.Set;

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
