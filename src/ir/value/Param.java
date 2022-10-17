package ir.value;

import ty.Ty;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Param extends Value {
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
