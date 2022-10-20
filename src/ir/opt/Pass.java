package ir.opt;

import ir.value.CompUnit;

public interface Pass {
    void run(CompUnit compUnit);
}
