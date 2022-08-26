package org.alloytools.alloy.absint.eval;

import org.alloytools.alloy.TestUtil;
import org.alloytools.alloy.absinst.Minimizer;
import org.alloytools.alloy.absinst.iter.MinimizerManualIterationTest;

public class EvalDummy {

    public static void main(String[] args) {
        Minimizer.DO_SANITY_CHECKS = false;
        TestUtil.disableSysOut();

        String module = args[0];

        MinimizerManualIterationTest.countInstances(module);

        TestUtil.restoreSysOut();
    }
}
