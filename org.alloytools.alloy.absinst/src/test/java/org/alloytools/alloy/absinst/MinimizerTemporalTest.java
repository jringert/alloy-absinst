package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.mit.csail.sdg.translator.A4Options.SatSolver;


public class MinimizerTemporalTest {

    @Test
    public void testEnumOneARun() {
        String module = "src/test/alloy/temporal/alternating.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum, UBKind.EXACT, SatSolver.SAT4J);

        assertEquals("[A$0]", m.getLowerBound().toString());
        assertEquals("[this/A ∌ A$1, A$2, ]", m.printUpperBound());
    }

}
