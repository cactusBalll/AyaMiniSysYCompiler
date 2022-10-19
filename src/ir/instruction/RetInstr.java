package ir.instruction;

import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;

public class RetInstr extends Instr{

    boolean isRetNull = false;
    public RetInstr(Value retValue) {
        super(new LinkedList<>(), new ArrayList<>());
        this.uses.add(retValue.getNode());
    }

    public boolean isRetNull() {
        return isRetNull;
    }

    public RetInstr() {
        super(new LinkedList<>(), new ArrayList<>());
        this.isRetNull = true;
    }
    public Value getRetValue() {
        if (!isRetNull) {
            return this.uses.get(0).getValue();
        } else {
            return null;
        }
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

    @Override
    public String toString() {
        if (!isRetNull) {
            return "ret " + getRetValue().getName();
        } else {
            return "ret";
        }

    }
}
