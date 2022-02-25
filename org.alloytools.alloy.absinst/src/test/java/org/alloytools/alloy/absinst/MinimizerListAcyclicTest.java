package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MinimizerListAcyclicTest {
	 @Test
    public void testListAcyclicRun() {
        String module = "src/test/alloy/list/list_acyclic.als";
        
        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        //Acyclic does not depend on the element value of nodes in the list 
        // Note: this produces the same output currently as MinimizerListTest which also uses no repetition predicate
        assertEquals("[List$0, List$0->Node$0, Node$0]", m.getLowerBound().toString());
        assertEquals("[UB for field (this/Node <: elem), UB for field (this/Node <: link)]", m.getUpperBound().toString());
    }
}
