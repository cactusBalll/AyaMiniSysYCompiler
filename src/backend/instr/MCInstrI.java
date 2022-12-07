package backend.instr;

import Driver.AyaConfig;
import backend.regs.PReg;
import backend.regs.Reg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MCInstrI extends MCInstr {
    public Reg t;
    public Reg s;
    public int imm;
    public Type type;

    @Override
    public List<Reg> getDef() {
        List<Reg> ret = new ArrayList<>();
        ret.add(t);
        return ret;
    }

    @Override
    public List<Reg> getUse() {
        List<Reg> ret = new ArrayList<>();
        ret.add(s);
        return ret;
    }

    @Override
    public void allocate(Reg vReg, Reg pReg) {
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
        sra,
        srl,
    }

    public MCInstrI(Reg t, Reg s, int imm, Type type) {
        this.t = t;
        this.s = s;
        this.imm = imm;
        this.type = type;
    }

    @Override
    public String toString() {
        if (AyaConfig.NO_IMM16_CHECK && type == Type.addu) {
            return String.format("addiu %s,%s,%d", t, s, imm);
        }
        return String.format("%s %s,%s,%d", type, t, s, imm);
    }
}
