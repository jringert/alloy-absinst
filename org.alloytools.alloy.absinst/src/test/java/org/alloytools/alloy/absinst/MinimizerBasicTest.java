package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class MinimizerBasicTest {

    @Test
    public void testOneARun() {
        String module = "src/test/alloy/basic/oneA.als";
        
        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        // trivial empty bounds because the run predicate is TRUE
        assertEquals("[A$0]", m.getLowerBound().toString());
        assertEquals("[UB for this/A]", m.getUpperBound().toString());
    }
    
    @Test
    public void testSomeARun() {
        String module = "src/test/alloy/basic/someA.als";
        
        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        // trivial empty bounds because the run predicate is TRUE
        assertEquals("[A$0]", m.getLowerBound().toString());
        assertEquals("[]", m.getUpperBound().toString());
    }
    

    @Test
    public void testEx2ARun() {
        String module = "src/test/alloy/basic/ex2A.als";
        
        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        // trivial empty bounds because the run predicate is TRUE
        assertEquals("[A$0, A$1]", m.getLowerBound().toString());
        assertEquals("[UB for this/A]", m.getUpperBound().toString());
    }


}
