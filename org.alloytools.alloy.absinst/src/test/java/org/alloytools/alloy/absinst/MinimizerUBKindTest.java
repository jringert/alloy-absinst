package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;


public class MinimizerUBKindTest {

    @Test
    public void testOneARun() {
        String module = "src/test/alloy/basic/oneA.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum, UBKind.INSTANCE);

        assertEquals("[A$0]", m.getLowerBound().toString());
        assertEquals("[UB for this/A]", m.getUpperBound().toString());

        m = MinimizerUtil.testMin(module, cmdNum, UBKind.INSTANCE_OR_NO_UPPER);

        assertEquals("[A$0]", m.getLowerBound().toString());
        assertEquals("[UB for this/A]", m.getUpperBound().toString());

        try {
            m = MinimizerUtil.testMin(module, cmdNum, UBKind.NO_UPPER);
            fail("UB kind NO_UPPER should be invalid here");
        } catch (Exception e) {
        }
    }

    @Test
    public void testSomeARun() {
        String module = "src/test/alloy/basic/someA.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum, UBKind.INSTANCE_OR_NO_UPPER);

        assertEquals("[A$0]", m.getLowerBound().toString());
        assertEquals("[]", m.getUpperBound().toString());

        m = MinimizerUtil.testMin(module, cmdNum, UBKind.INSTANCE);

        assertEquals("[A$0]", m.getLowerBound().toString());
        assertEquals("[UB for this/A]", m.getUpperBound().toString());


        m = MinimizerUtil.testMin(module, cmdNum, UBKind.NO_UPPER);

        assertEquals("[A$0]", m.getLowerBound().toString());
        assertEquals("[]", m.getUpperBound().toString());
    }


    @Test
    public void testEx2ARun() {
        String module = "src/test/alloy/basic/ex2A.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum, UBKind.INSTANCE_OR_NO_UPPER);

        assertEquals("[A$0, A$1]", m.getLowerBound().toString());
        assertEquals("[UB for this/A]", m.getUpperBound().toString());

        m = MinimizerUtil.testMin(module, cmdNum, UBKind.INSTANCE);

        assertEquals("[A$0, A$1]", m.getLowerBound().toString());
        assertEquals("[UB for this/A]", m.getUpperBound().toString());

        try {
            m = MinimizerUtil.testMin(module, cmdNum, UBKind.NO_UPPER);
            fail("UB kind NO_UPPER should be invalid here");
        } catch (Exception e) {
        }
    }


    @Test
    public void testOneAtwoBRun() {
        String module = "src/test/alloy/basic/oneAtwoB.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum, UBKind.INSTANCE_OR_NO_UPPER);

        assertEquals("[A$0, B$0, B$1]", m.getLowerBound().toString());
        assertEquals("[UB for this/A, UB for this/B]", m.getUpperBound().toString());

        m = MinimizerUtil.testMin(module, cmdNum, UBKind.INSTANCE);

        assertEquals("[A$0, B$0, B$1]", m.getLowerBound().toString());
        assertEquals("[UB for this/A, UB for this/B]", m.getUpperBound().toString());

        try {
            m = MinimizerUtil.testMin(module, cmdNum, UBKind.NO_UPPER);
            fail("UB kind NO_UPPER should be invalid here");
        } catch (Exception e) {
        }
    }

    @Test
    public void testOneABsubRun() {
        String module = "src/test/alloy/basic/oneABsub.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum, UBKind.INSTANCE_OR_NO_UPPER);

        assertEquals("[A$0]", m.getLowerBound().toString());
        assertEquals("[UB for this/B, UB for this/A]", m.getUpperBound().toString());

        m = MinimizerUtil.testMin(module, cmdNum, UBKind.INSTANCE);

        assertEquals("[A$0]", m.getLowerBound().toString());
        assertEquals("[UB for this/B, UB for this/A]", m.getUpperBound().toString());

        try {
            m = MinimizerUtil.testMin(module, cmdNum, UBKind.NO_UPPER);
            fail("UB kind NO_UPPER should be invalid here");
        } catch (Exception e) {
        }
    }
}
