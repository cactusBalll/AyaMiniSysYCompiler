package backend;

import backend.instr.Label;
import ir.value.BasicBlock;

/**
 * 用于翻译过程
 */
public class PsuMCBlock extends MCBlock{
    public BasicBlock basicBlock;

    public PsuMCBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
        this.label = new Label("Pseudo",null);
    }

    @Override
    public String toString() {
        return "PsuMCBlock";
    }
}
