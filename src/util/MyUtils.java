package util;

import java.util.List;
import java.util.function.Function;

public class MyUtils {
    public static String formatList(List list) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Object o :
                list) {
            sb.append(o.toString()).append(", ");
        }
        sb.append(']');
        return sb.toString();
    }

    public static String formatList(List list, Function<Object, String> mapper) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Object o :
                list) {
            sb.append(mapper.apply(o)).append(", ");
        }
        sb.append(']');
        return sb.toString();
    }

}
