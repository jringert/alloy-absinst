package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;


public class MinimizerListTest extends MinimizerTest {

    @Test
    public void testListRun() {
        String module = "src/test/alloy/list/list.als";
        
        int cmdNum = 0;

        Minimizer m = testMin(module, cmdNum);

        // trivial empty bounds because the run predicate is TRUE
        assertTrue(m.getLowerBound().isEmpty());
        assertTrue(m.getUpperBound().isEmpty());
    }

}
