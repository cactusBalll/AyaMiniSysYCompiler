package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;

import java.util.HashSet;
import java.util.Set;

public class MCInstrI extends MCInstr {
    public Reg t;
    public Reg s;
    public int imm;
    public Type type;

    @Override
    public Set<Reg> getDef() {
        Set<Reg> ret = new HashSet<>();
        ret.add(t);
        return ret;
    }

    @Override
    public Set<Reg> getUse() {
        Set<Reg> ret = new HashSet<>();
        ret.add(s);
        return ret;
    }

    @Override
    public void allocate(Reg vReg, PReg pReg) {
        if (s == vReg) {
            s = pReg;
        }
        if (t == vReg) {
            t = pReg;
        }
    }

    public enum Type {
        addu, //pseudo
        subu, //pseudo
        mul, //pseudo

        sge, //pseudo
        sgt, //pseudo
        sle, //pseudo
        seq, //pseudo
        sne, //pseudo

        addiu,
        slti,
        xori,

        sll,
    }

    public MCInstrI(Reg t, Reg s, int imm, Type type) {
        this.t = t;
        this.s = s;
        this.imm = imm;
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("%s %s,%s,%d", type, t, s, imm);
    }
}
