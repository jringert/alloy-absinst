package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class MinimizerListTest {

    @Test
    public void testListRun() {
        String module = "src/test/alloy/list/list.als";
        
        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        // FIXME no clue if this is correct
        // it depends on the instance we generate this from
        assertEquals("[List$0, List$0->Node$0, Node$0]", m.getLowerBound().toString());
        assertEquals("[UB for field (this/Node <: elem), UB for field (this/Node <: link)]", m.getUpperBound().toString());
    }

}
