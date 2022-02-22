package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class MinimizerListTest {

    @Test
    public void testListRun() {
        String module = "src/test/alloy/list/list.als";
        
        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        // trivial empty bounds because the run predicate is TRUE
        assertTrue(m.getLowerBound().isEmpty());
        assertTrue(m.getUpperBound().isEmpty());
    }

}
