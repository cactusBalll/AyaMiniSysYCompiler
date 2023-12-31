package ir.instruction;

import ir.value.BasicBlock;
import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;

public class Instr extends User {
    public Instr(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public Instr(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public Instr(Instr rhs) {
        super(rhs);
        this.isNop = rhs.isNop;
    }

    public Instr(Ty type, String name) {
        super(type, name);
    }



    public boolean isNop = false;
}
