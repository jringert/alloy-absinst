package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MinimizerListAcyclicTest {
	 @Test
    public void testListAcyclicRun() {
        String module = "src/test/alloy/list/list_acyclic.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);


        assertEquals("[]", m.getLowerBound().toString());
        assertEquals("[UB for field (this/Node <: link)]", m.getUpperBound().toString());
    }
}
