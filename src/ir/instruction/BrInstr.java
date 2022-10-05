package ir.instruction;

import ir.value.BasicBlock;
import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;

public class BrInstr extends Instr{

    public BrInstr(Value cond, BasicBlock br0, BasicBlock br1) {
        super(new LinkedList<>(), new ArrayList<>());
        this.uses.add(cond.getNode());
        this.uses.add(br0.getNode());
        this.uses.add(br1.getNode());
    }

    public Value getCond() {
        return this.uses.get(0).getValue();
    }

    public BasicBlock getBr0() {
        return (BasicBlock) this.uses.get(1).getValue();
    }

    public BasicBlock getBr1() {
        return (BasicBlock) this.uses.get(2).getValue();
    }

    public BrInstr(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public BrInstr(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public BrInstr(User rhs) {
        super(rhs);
    }

    public BrInstr(Ty type, String name) {
        super(type, name);
    }
}
