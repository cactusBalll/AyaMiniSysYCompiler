package ir.value;

import ty.FuncTy;
import ty.Ty;
import util.MyList;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Function extends User{


    private final List<Param> params = new ArrayList<>();

    public List<Param> getParams() {
        return params;
    }

    public Function(Ty ty, String name, List<Param> params) {
        super(new LinkedList<>(), new ArrayList<>());
        this.setType(ty);
        this.name = name;
        this.params.addAll(params);
    }

    private final MyList<BasicBlock> list = new MyList<>();

    public MyList<BasicBlock> getList() {
        return list;
    }

    public BasicBlock getFirstBB() {
        return list.getFirst().getValue();
    }

    public Function(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public Function(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public Function(User rhs) {
        super(rhs);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append('(');
        for (Param p :
                params) {
            sb.append(p.name).append(':').append(p.getType()).append(',');
        }
        sb.append("):").append(((FuncTy)getType()).getRet());
        sb.append("{\n");
        for (MyNode<BasicBlock> bbNode :
                list) {
            sb.append(bbNode.getValue());
        }
        sb.append("\n}");

        return sb.toString();
    }
}
