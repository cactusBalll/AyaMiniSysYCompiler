package ir.value;

import Driver.AyaConfig;
import ir.instruction.Instr;
import jdk.nashorn.internal.runtime.regexp.joni.Config;
import ty.Ty;
import util.MyList;
import util.MyNode;
import util.MyUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BasicBlock extends Value{

    public BasicBlock idomer;
    public List<BasicBlock> idoming = new ArrayList<>();
    public List<BasicBlock> domer = new ArrayList<>();
    public List<BasicBlock> doming = new ArrayList<>();
    public int domDepth;

    //支配边界
    public List<BasicBlock> df = new ArrayList<>();

    public List<BasicBlock> prec = new ArrayList<>();
    public List<BasicBlock> succ = new ArrayList<>();

    public int loopDepth;

    public void setList(MyList<Instr> list) {
        this.list = list;
    }

    public void clearBBInfo() {
        idomer = null;
        idoming.clear();
        domer.clear();
        doming.clear();
        domDepth = 0;
        df.clear();
    }

    public void clearPrecSucc() {
        prec.clear();
        succ.clear();
    }

    private MyList<Instr> list = new MyList<>();

    public MyList<Instr> getList() {
        return list;
    }

    public void addInstr(Instr instr) {
        list.add(instr.getNode());
    }

    public void addInstrAtFirst(Instr instr) {
        list.addFirst(instr.getNode());
    }

    public BasicBlock() {
        super(new LinkedList<>());
    }

    public BasicBlock(LinkedList<MyNode<User>> users, Ty type) {
        super(users, type);
    }

    public BasicBlock(BasicBlock rhs) {
        super(rhs);
        this.list = rhs.list;
        this.loopDepth = rhs.loopDepth;
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
        if (AyaConfig.PRINT_IR_BB_INFO) {
            sb.append("prec: ").append(MyUtils.formatList(prec,(o)->((BasicBlock)o).getName())).append('\n');
            sb.append("succ: ").append(MyUtils.formatList(succ,(o)->((BasicBlock)o).getName())).append('\n');
            sb.append("domer: ").append(MyUtils.formatList(domer,(o)->((BasicBlock)o).getName())).append('\n');
            sb.append("doming: ").append(MyUtils.formatList(doming,(o)->((BasicBlock)o).getName())).append('\n');
            sb.append("idomer: ").append(idomer!=null?idomer.getName():"none").append('\n');
            sb.append("idoming: ").append(MyUtils.formatList(idoming,(o)->((BasicBlock)o).getName())).append('\n');
            sb.append("df: ").append(MyUtils.formatList(df,(o)->((BasicBlock)o).getName())).append('\n');
            sb.append("domDepth: ").append(domDepth).append('\n');
            sb.append("loopDepth: ").append(loopDepth).append('\n');
        }

        for (MyNode<Instr> instrNode:
            list){
            sb.append('\t').append(instrNode.getValue().toString()).append('\n');
        }
        return sb.toString();
    }
}
