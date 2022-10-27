package backend.instr;

import backend.regs.Reg;

import java.util.HashSet;
import java.util.Set;


public class MCInstrR extends MCInstr {
    public Reg d;
    public Reg s;
    public Reg t;
    public Type type;

    @Override
    public Set<Reg> getDef() {
        Set<Reg> ret = new HashSet<>();
        ret.add(d);
        return ret;
    }

    @Override
    public Set<Reg> getUse() {
        Set<Reg> ret = new HashSet<>();
        ret.add(s);
        ret.add(t);
        return ret;
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
            return type.toString() + ' ' + ',' + s.toString() + ',' + t.toString();
        }
    }
}
