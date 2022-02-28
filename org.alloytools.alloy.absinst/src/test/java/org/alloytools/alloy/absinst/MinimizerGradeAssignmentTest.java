package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MinimizerGradeAssignmentTest {
    @Test
    public void testGradeAssignmentRun() {
        String module = "src/test/alloy/small_models/gradeFaulty.als";
        
        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        // looks correct
        // all students are of the class of the assignment (third person in scope must be professor)
        // one of the two students must be taking the assignment (because it has mult. some)
        // not a great example to make our case...
        assertEquals("[Student$0, Student$1, Class$0, Class$0->Student$0, Class$0->Student$1, Assignment$2, Assignment$2->Class$0]", m.getLowerBound().toString());
        assertEquals("[]", m.getUpperBound().toString());
    }
}
