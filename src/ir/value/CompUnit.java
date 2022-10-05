package ir.value;

import util.MyList;

public class CompUnit {

    private final MyList<Value> globalValueList = new MyList<>();
    private final MyList<Function> list = new MyList<>();

    public MyList<Function> getList() {
        return list;
    }

    public MyList<Value> getGlobalValueList() {
        return globalValueList;
    }
}
