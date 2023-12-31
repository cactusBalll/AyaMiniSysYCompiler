package ir.value;

import java.util.LinkedList;

public class MyString extends Value{
    private final String str;
    public MyString(String str) {
        super(new LinkedList<>());
        this.str = str;
    }

    public MyString(MyString rhs) {
        super(rhs);
        str = rhs.str;
    }
    public String getStr() {
        return str;
    }

    @Override
    public String toString() {
        return "\""+str+"\"";
    }
}
