package ir.value;

import ir.instruction.Instr;
import ty.Ty;
import util.MyList;
import util.MyNode;

import java.util.LinkedList;

public class BasicBlock extends Value{

    private final MyList<Instr> list = new MyList<>();

    public MyList<Instr> getList() {
        return list;
    }

    public void addInstr(Instr instr) {
        list.add(instr.getNode());
    }

    public BasicBlock() {
        super(new LinkedList<>());
    }

    public BasicBlock(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public BasicBlock(Value rhs) {
        super(rhs);
    }

    public BasicBlock(Ty type, String name) {
        super(type, name);
    }

    public BasicBlock(Ty type) {
        super(type);
    }

    public BasicBlock(LinkedList<MyNode<User>> users) {
        super(users);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(":\n");
        for (MyNode<Instr> instrNode:
            list){
            sb.append('\t').append(instrNode.getValue().toString()).append('\n');
        }
        return sb.toString();
    }
}
