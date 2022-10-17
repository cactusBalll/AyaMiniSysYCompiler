package ir.instruction;

import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class StoreInstr extends Instr{
    public StoreInstr(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public StoreInstr(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public StoreInstr(User rhs) {
        super(rhs);
    }

    public StoreInstr(Ty type, String name) {
        super(type, name);
    }

    public StoreInstr(Value ptr, Value target, Value index) {
        super(new LinkedList<>(), new ArrayList<>());
        this.uses.add(ptr.getNode());
        this.uses.add(target.getNode());
        this.uses.add(index.getNode());
    }

    public Value getPtr() {
        return this.uses.get(0).getValue();
    }

    public Value getTarget() {
        return this.uses.get(1).getValue();
    }

    public Value getIndex() {
        return this.uses.get(2).getValue();
    }
}
