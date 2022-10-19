package ir.value;

import ty.Ty;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Param extends Value {
    /**
     * 是否应该以load，store形式访问，mem2reg后这个值被设成false
     */
    private boolean isLoadStore = true;

    public boolean isLoadStore() {
        return isLoadStore;
    }

    public void setLoadStore(boolean loadStore) {
        isLoadStore = loadStore;
    }

    public Param(Ty type) {
        super(new LinkedList<>(), type);
    }
    public static List<Ty> extractType(List<Param> params) {
        List<Ty> ret = new ArrayList<>();
        for (Param p :
                params) {
            ret.add(p.getType());
        }
        return ret;
    }

}
