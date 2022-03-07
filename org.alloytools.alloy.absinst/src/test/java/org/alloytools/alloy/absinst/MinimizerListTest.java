package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class MinimizerListTest {

    @Test
    public void testListRun() {
        String module = "src/test/alloy/list/list.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        assertEquals("[]", m.getLowerBound().toString());
        assertEquals("[UB for field (this/Node <: link)]", m.getUpperBound().toString());
    }

    @Test
    public void testListCycleRun() {
        String module = "src/test/alloy/list/list_cycle.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        assertEquals("[Node$0, Node$0->Node$0]", m.getLowerBound().toString());
        assertEquals("[]", m.getUpperBound().toString());
    }

}
