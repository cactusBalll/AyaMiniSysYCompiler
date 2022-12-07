package ir.instruction;

import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LoadInstr extends Instr{

    public LoadInstr(Value ptr, Value index) {
        super(new LinkedList<>(), new ArrayList<>());
        this.uses.add(ptr.getNode());
        this.uses.add(index.getNode());
    }

    public Value getPtr() {
        return this.uses.get(0).getValue();
    }

    public Value getIndexes() {
        return this.uses.get(1).getValue();
    }

    public LoadInstr(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public LoadInstr(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public LoadInstr(LoadInstr rhs) {
        super(rhs);
    }

    public LoadInstr(Ty type, String name) {
        super(type, name);
    }

    @Override
    public String toString() {
        return name + "= load " + getPtr().getName() + '[' + getIndexes().getName() + ']';
    }
}
