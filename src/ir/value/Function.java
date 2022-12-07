package ir.value;

import ty.FuncTy;
import ty.Ty;
import util.MyList;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class Function extends User{

    private boolean isPure = false;
    private boolean isRecursive = false;

    public void setPure(boolean pure) {
        isPure = pure;
    }

    public void setRecursive(boolean recursive) {
        isRecursive = recursive;
    }

    public void setList(MyList<BasicBlock> list) {
        this.list = list;
    }

    public boolean isPure() {
        return isPure;
    }

    public boolean isRecursive() {
        return isRecursive;
    }

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

    private MyList<BasicBlock> list = new MyList<>();

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

    public Function(Function rhs) {
        super(rhs);
        this.isPure = rhs.isPure;
        this.isRecursive = rhs.isRecursive;
        this.list = rhs.list;
        this.params.addAll(rhs.params);
    }

    public void forEveryBasicBlock(Consumer<BasicBlock> f) {
        for (MyNode<BasicBlock> bbNode :
                list) {
            BasicBlock bb = bbNode.getValue();
            f.accept(bb);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('%').append(name);
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
