package ir.value;

import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;

public abstract class Value {

    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    protected LinkedList<MyNode<User>> users; // 这个值的使用者
    private Ty type;

    //反正都是Object，Java泛型也是半残
    private final MyNode node = new MyNode<>(this);

    public MyNode getNode() {
        return node;
    }

    public LinkedList<MyNode<User>> getUsers() {
        return users;
    }


    public Value(LinkedList<MyNode<User>> users, Ty type) {
        this.users = users;
        this.type = type;
    }


    public Value(Value rhs) {
        users = (LinkedList<MyNode<User>>) rhs.users.clone();
        type = rhs.type; // type是不可变类型
    }
    public Value(Ty type, String name) {
        this.type = type;
        this.users = new LinkedList<>();
    }

    public Value(Ty type) {
        this.type = type;
        this.users = new LinkedList<>();
    }

    public void addUser(User user) {
        users.add(user.getNode());
    }

    public void resetUsers() {
        users = new LinkedList<>();
    }
    public Value(LinkedList<MyNode<User>> users) {
        this.users = users;
    }

    public Ty getType() {
        return type;
    }


    public void setType(Ty type) {
        this.type = type;
    }

    public void removeMeAndMyUsers() {
        users.forEach(MyNode::removeMe);
        getNode().removeMe();
    }

    public void replaceAllUsesOfMeWith(Value other) {
        ArrayList<User> usersToReplace = new ArrayList<User>();
        for (MyNode<User> userNode :
                users) {
            User user = userNode.getValue();
            usersToReplace.add(user);
            other.addUser(user);
        }
        for (User user :
                usersToReplace) {
            user.replaceUseWith(this, other);

        }
    }

}
