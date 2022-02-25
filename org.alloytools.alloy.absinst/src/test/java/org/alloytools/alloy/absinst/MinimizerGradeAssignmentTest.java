package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MinimizerGradeAssignmentTest {
    @Test
    public void testGradeAssignmentRun() {
        String module = "src/test/alloy/small_models/gradeFaulty.als";
        
        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        // TODO look into this result
        //check command
        //Model is faulty and should not produce counterexample but does
        //Does the minimizer help highlight whats wrong?
        assertEquals("[Student$0, Student$1, Class$0, Class$0->Student$0, Class$0->Student$1, Assignment$2, Assignment$2->Class$0]", m.getLowerBound().toString());
        assertEquals("[]", m.getUpperBound().toString());
    }
}
