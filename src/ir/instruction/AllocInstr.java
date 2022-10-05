package ir.instruction;

import ir.value.User;
import ir.value.Value;
import ty.Ty;
import util.MyNode;

import java.util.ArrayList;
import java.util.LinkedList;

public class AllocInstr extends Instr{

    public enum AllocType{
        Static,
        Stack
    }

    private AllocType allocType;
    private Ty allocTy;

    public AllocInstr(Ty allocTy, AllocType allocType) {
        super(new LinkedList<>(), new ArrayList<>());
        this.allocType = allocType;
        this.allocTy = allocTy;
    }
    public AllocInstr(LinkedList<MyNode<User>> users, ArrayList<Value> uses) {
        super(users, uses);
    }

    public AllocInstr(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public AllocInstr(User rhs) {
        super(rhs);
    }

    public AllocInstr(Ty type, String name) {
        super(type, name);
    }
}
