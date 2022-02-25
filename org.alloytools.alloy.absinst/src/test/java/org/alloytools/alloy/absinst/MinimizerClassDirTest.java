package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MinimizerClassDirTest {
    @Test
    public void testClassDirectoryRun() {
        String module = "src/test/alloy/small_models/classDir.als";
        
        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        // TODO look into this result
        assertEquals("[]", m.getLowerBound().toString());
        assertEquals("[UB for this/Class, UB for field (this/Class <: ext)]", m.getUpperBound().toString());
    }
}
