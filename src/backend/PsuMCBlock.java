package backend;

import ir.value.BasicBlock;

/**
 * 用于翻译过程
 */
public class PsuMCBlock extends MCBlock{
    public BasicBlock basicBlock;

    public PsuMCBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }
}
