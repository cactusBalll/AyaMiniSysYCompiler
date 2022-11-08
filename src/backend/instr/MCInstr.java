package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;
import util.MyNode;

import java.util.HashSet;
import java.util.Set;

public abstract class MCInstr {
    private final MyNode node = new MyNode<>(this);

    public Set<Reg> getDef() {
        return new HashSet<>();
    }
    public Set<Reg> getUse() {
        return new HashSet<>();
    }
    public MyNode getNode() {
        return node;
    }

    public abstract void allocate(Reg vReg, Reg pReg);
}
