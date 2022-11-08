package backend.coloralloc;

import backend.MCFunction;
import backend.MCUnit;

public class ColorAllocDriver {
    public static void run(MCUnit mcUnit) {
        mcUnit.list.forEach(fNode->{
            MCFunction f = fNode.getValue();
            ColorAllocator allocator = new ColorAllocator(f);
            allocator.run();
        });
    }
}
