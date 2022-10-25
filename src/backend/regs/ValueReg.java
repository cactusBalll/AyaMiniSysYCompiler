package backend.regs;

import ir.value.Value;

/**
 * 翻译代码时存储临时的Reg，Value对应关系
 */
public class ValueReg extends Reg{
    public Value value;

    public ValueReg(Value value) {
        this.value = value;
    }
}
