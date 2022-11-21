package util;

import backend.coloralloc.IGNode;

import java.util.HashSet;
import java.util.Set;

public class SetUtil<T> {
    public  Set<T> union(Set<T> u, Set<T> v) {
        Set<T> ret = new HashSet<>(u);
        ret.addAll(v);
        return ret;
    }

    public static SetUtil<IGNode> ofIGNode() {
        return igNodeSetUtil;
    }

    private static SetUtil<IGNode> igNodeSetUtil = new SetUtil<>();
}
