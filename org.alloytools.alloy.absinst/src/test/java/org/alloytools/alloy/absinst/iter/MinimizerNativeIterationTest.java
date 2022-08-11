package org.alloytools.alloy.absinst.iter;

import static org.junit.Assert.assertEquals;

import org.alloytools.alloy.absinst.Minimizer;
import org.alloytools.alloy.absinst.MinimizerUtil;
import org.junit.Test;


public class MinimizerNativeIterationTest {

    private static int abstractInstanceNum;

    @Test
    public void testSomeARun() {
        String module = "src/test/alloy/basic/someA.als";

        Minimizer m = MinimizerUtil.testMin(module, 0);
        abstractInstanceNum = 1;
        while (m.next()) {
            abstractInstanceNum++;
        }
        assertEquals(1, abstractInstanceNum);
    }
    //
    //    @Test
    //    public void testAddressBook3a() {
    //        String module = "../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3a.als";
    //
    //
    //    }
}
