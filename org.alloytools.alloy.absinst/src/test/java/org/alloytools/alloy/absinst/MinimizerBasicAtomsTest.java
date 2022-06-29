package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class MinimizerBasicAtomsTest {

    @Test
    public void testOneARun() {
        String module = "src/test/alloy/basic/oneA.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        assertEquals("[A$0]", m.getLowerBound().toString());
        assertEquals("[UB for this/A]", m.getUpperBound().toString());
    }

    @Test
    public void testOneARunExact() {
        String module = "src/test/alloy/basic/oneA.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum, UBKind.EXACT);

        assertEquals("[A$0]", m.getLowerBound().toString());
        assertEquals("[UB for this/A]", m.getUpperBound().toString());
    }

    @Test
    public void testSomeARun() {
        String module = "src/test/alloy/basic/someA.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        assertEquals("[A$0]", m.getLowerBound().toString());
        assertEquals("[]", m.getUpperBound().toString());
    }


    @Test
    public void testEx2ARun() {
        String module = "src/test/alloy/basic/ex2A.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        assertEquals("[A$0, A$1]", m.getLowerBound().toString());
        assertEquals("[UB for this/A]", m.getUpperBound().toString());
    }


    @Test
    public void testOneAtwoBRun() {
        String module = "src/test/alloy/basic/oneAtwoB.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        assertEquals("[A$0, B$0, B$1]", m.getLowerBound().toString());
        assertEquals("[UB for this/A, UB for this/B]", m.getUpperBound().toString());
    }

    @Test
    public void testOneABsubRun() {
        String module = "src/test/alloy/basic/oneABsub.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        assertEquals("[A$0]", m.getLowerBound().toString());
        assertEquals("[UB for this/B, UB for this/A]", m.getUpperBound().toString());
    }
}
