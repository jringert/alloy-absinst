package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class MinimizerBasicTupleTest {

    @Test
    public void testCycleARun() {
        String module = "src/test/alloy/basic/cycleA.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        assertEquals("[A$0, A$1, A$0->A$1, A$1->A$0]", m.getLowerBound().toString());
        assertEquals("[UB for field (this/A <: nx)]", m.getUpperBound().toString());
    }

    @Test
    public void testCycleARunExact() {
        String module = "src/test/alloy/basic/cycleA.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum, UBKind.EXACT);

        assertEquals("[A$0, A$1, A$0->A$1, A$1->A$0]", m.getLowerBound().toString());
        assertEquals("[field (this/A <: nx) ∌ A$0 -> A$0, A$1 -> A$1, ]", m.printUpperBound());
    }

}
