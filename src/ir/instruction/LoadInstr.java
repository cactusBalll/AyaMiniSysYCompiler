package ir.instruction;

import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LoadInstr extends Instr{

    public LoadInstr(Value ptr, List<Value> indexes) {
        super(new LinkedList<>(), new ArrayList<>());
        this.uses.add(ptr.getNode());
        for (Value v :
                indexes) {
            this.uses.add(v.getNode());
        }
    }

    public Value getPtr() {
        return this.uses.get(0).getValue();
    }

    public List<Value> getIndexes() {
        List<Value> indexes = new ArrayList<>();
        for (int i = 1; i < uses.size(); i++) {
            indexes.add(uses.get(i).getValue());
        }
        return indexes;
    }

    public LoadInstr(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public LoadInstr(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public LoadInstr(User rhs) {
        super(rhs);
    }

    public LoadInstr(Ty type, String name) {
        super(type, name);
    }
}
