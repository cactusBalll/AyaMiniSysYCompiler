package ir.instruction;

import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 我好像写重了。。
 */
@Deprecated
public class BranchInstr extends Instr{

    enum BranchType{
        Conditional,
        Goto
    }

    private BranchType branchType;

    public BranchInstr(Value target) {
        super(new LinkedList<>(), new ArrayList<>());
        uses.add(target.getNode());
        this.branchType = BranchType.Goto;
    }

    public BranchInstr(Value cond,Value branch1, Value branch2) {
        super(new LinkedList<>(), new ArrayList<>());
        uses.add(cond.getNode());
        uses.add(branch1.getNode());
        uses.add(branch2.getNode());
        this.branchType = BranchType.Conditional;
    }

    public Value getCond() {
        if (branchType == BranchType.Goto) {
            return null;
        } else {
            return uses.get(0).getValue();
        }
    }

    public List<Value> getBranches() {
        List<Value> branches = new ArrayList<>();
        if (branchType == BranchType.Goto) {
            branches.add(uses.get(0).getValue());
        } else {
            branches.add(uses.get(1).getValue());
            branches.add(uses.get(2).getValue());
        }
        return branches;
    }

    public BranchInstr(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public BranchInstr(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public BranchInstr(User rhs) {
        super(rhs);
    }

    public BranchInstr(Ty type, String name) {
        super(type, name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("br ");
        sb.append(getCond().getName()).append(',');
        sb.append(getBranches().get(0).getName()).append(',');
        sb.append(getBranches().get(1).getName());
        return sb.toString();
    }
}
