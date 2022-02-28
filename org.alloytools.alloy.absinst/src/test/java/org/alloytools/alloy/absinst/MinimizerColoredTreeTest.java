package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MinimizerColoredTreeTest {
	
    @Test
    public void testBasicColoredTreeRun() {
        String module = "src/test/alloy/small_models/ctree.als";
        
        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        // TODO look into this result
        assertEquals("[]", m.getLowerBound().toString());
        assertEquals("[]", m.getUpperBound().toString());
    }
}
