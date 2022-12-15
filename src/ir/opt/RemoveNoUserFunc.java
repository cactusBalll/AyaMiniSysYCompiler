package ir.opt;

import ir.value.CompUnit;
import ir.value.Function;

import java.util.ArrayList;
import java.util.List;

public class RemoveNoUserFunc implements Pass {
    @Override
    public void run(CompUnit compUnit) {
        List<Function> queueFree = new ArrayList<>();
        compUnit.forEveryFunction(function -> {
            if (function.getUsers().isEmpty() && !function.getName().equals("main")) {
                queueFree.add(function);
            }
        });
        queueFree.forEach(function -> function.getNode().removeMe());
    }
}
