package ir.instruction;

import ir.value.BasicBlock;
import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;
import util.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PhiInstr extends Instr{

    public PhiInstr(List<Pair<Value, BasicBlock>> pairs) {
        super(new LinkedList<>(), new ArrayList<>());
        for (Pair<Value, BasicBlock> pair :
                pairs) {
            uses.add(pair.getFirst().getNode());
            uses.add(pair.getLast().getNode());
        }
    }

    public List<Pair<Value, BasicBlock>> getPhiPairs() {
        List<Pair<Value, BasicBlock>> phiPairs = new ArrayList<>();
        for (int i = 0, usesSize = uses.size(); i < usesSize; i += 2) {
            phiPairs.add(new Pair<>(uses.get(i).getValue(), (BasicBlock) uses.get(i+1).getValue()));
        }

        return  phiPairs;
    }
    public PhiInstr(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public PhiInstr(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public PhiInstr(User rhs) {
        super(rhs);
    }

    public PhiInstr(Ty type, String name) {
        super(type, name);
    }
}
