package ir.value;

import ty.Ty;
import util.MyList;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Function extends User{

    private String name;

    private final List<Value> params = new ArrayList<>();

    public Function(Ty ty, String name) {
        super(new LinkedList<>(), new ArrayList<>());
        this.setType(ty);
        this.name = name;
    }

    private final MyList<BasicBlock> list = new MyList<>();

    public MyList<BasicBlock> getList() {
        return list;
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

}
