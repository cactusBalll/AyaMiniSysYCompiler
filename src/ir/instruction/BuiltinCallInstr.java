package ir.instruction;

import ir.value.User;
import ir.value.Value;

import java.util.ArrayList;
import java.util.LinkedList;

public class BuiltinCallInstr extends Instr {
    public enum Func{
        PutStr,
        PutInt,
        GetInt
    }

    private final Func func;
    public BuiltinCallInstr(Func func, Value param) {
        super(new LinkedList<>(),new ArrayList<>());
        this.func = func;
        uses.add(param.getNode());
    }

    public Func getFunc() {
        return func;
    }

    public Value getParam() {
        return uses.get(0).getValue();
    }

    public BuiltinCallInstr(BuiltinCallInstr rhs) {
        super(rhs);
        this.func = rhs.func;
    }
    @Override
    public String toString() {
        switch (func) {
            case PutInt:
                return "putint("+ getParam().getName() +')';
            case GetInt:
                return getName() + " = getint()";
            case PutStr:
                return "putstr(" + getParam().toString() + ')';
        }
        return "";
    }
}
