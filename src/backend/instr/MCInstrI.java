package backend.instr;

import backend.regs.Reg;

public class MCInstrI extends MCInstr {
    public Reg t;
    public Reg s;
    public int imm;
    public Type type;

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
