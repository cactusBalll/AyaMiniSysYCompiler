package ir.instruction;

import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;

public class RetInstr extends Instr{

    public RetInstr(Value retValue) {
        super(new LinkedList<>(), new ArrayList<>());
        this.uses.add(retValue.getNode());
    }

    public Value getRetValue() {
        return this.uses.get(0).getValue();
    }

    public RetInstr(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public RetInstr(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public RetInstr(User rhs) {
        super(rhs);
    }

    public RetInstr(Ty type, String name) {
        super(type, name);
    }
}
