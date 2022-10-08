package ir.value;

import ty.Ty;

import java.util.LinkedList;

public class Param extends Value {
    public Param(Ty type) {
        super(new LinkedList<>(), type);
    }
}
