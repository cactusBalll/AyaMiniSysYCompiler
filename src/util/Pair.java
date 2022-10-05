package util;

public class Pair<T,U> {
    private final T first;
    private final U last;

    public Pair(T t,U u) {
        first = t;
        last = u;
    }

    public T getFirst() {
        return first;
    }

    public U getLast() {
        return last;
    }
}
