package ir.value;

import java.util.LinkedList;

public class MyString extends Value{
    private final String str;
    public MyString(String str) {
        super(new LinkedList<>());
        this.str = str;
    }
}
