package backend.instr;

import backend.regs.PReg;
import backend.regs.Reg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MCInstrR extends MCInstr {
    public Reg d;
    public Reg s;
    public Reg t;
    public Type type;

    @Override
    public List<Reg> getDef() {
        List<Reg> ret = new ArrayList<>();
        if (d != null) {
            ret.add(d);
        }
        return ret;
    }

    @Override
    public List<Reg> getUse() {
        List<Reg> ret = new ArrayList<>();
        ret.add(s);
        ret.add(t);
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
        if (d == vReg) {
            d = pReg;
        }
    }

    public enum Type {
        addu,
        subu,
        div,
        mult,

        and,
        or,
        sll,
        srl,
        slt,
        sltu,

        mul,

        sge, //pseudo
        sgt, //pseudo
        sle, //pseudo
        seq, //pseudo
        sne, //pseudo

    }

    public MCInstrR(Reg d, Reg s, Reg t, Type type) {
        this.d = d; //may be null
        this.s = s;
        this.t = t;
        this.type = type;
    }

    @Override
    public String toString() {
        if (d != null) {
            return type.toString() + ' ' + d.toString() + ',' + s.toString() + ',' + t.toString();
        } else {
            return type.toString() + ' ' + s.toString() + ',' + t.toString();
        }
    }
}
