package ir.instruction;

import ir.value.BasicBlock;
import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;

public class JmpInstr extends Instr{

    public JmpInstr(BasicBlock target) {
        super(new LinkedList<>(), new ArrayList<>());
        uses.add(target.getNode());
    }

    public BasicBlock getTarget() {
        return (BasicBlock) uses.get(0).getValue();
    }
    public JmpInstr(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public JmpInstr(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public JmpInstr(User rhs) {
        super(rhs);
    }

    public JmpInstr(Ty type, String name) {
        super(type, name);
    }

    @Override
    public String toString() {
        return "jmp " + getTarget().getName();
    }
}
