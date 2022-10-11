package ir.value;

import ir.instruction.AllocInstr;
import util.MyList;

public class CompUnit {

    private final MyList<AllocInstr> globalValueList = new MyList<>();
    private final MyList<Function> list = new MyList<>();

    public MyList<Function> getList() {
        return list;
    }

    public MyList<AllocInstr> getGlobalValueList() {
        return globalValueList;
    }

    public void addGlobalValue(AllocInstr allocInstr) {
        globalValueList.add(allocInstr.getNode());
    }
}
