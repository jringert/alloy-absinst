package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class MinimizerTest {

    @Test
    public void testAddrBook1aRun() {
        String module = "../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1a.als";

        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        // trivial empty bounds because the run predicate is TRUE
        assertTrue(m.getLowerBound().isEmpty());
        assertTrue(m.getUpperBound().isEmpty());
    }

    @Test
    public void testAddrBook1bRun() {
        String module = "../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1b.als";
        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        // manually checked this output for validity
        assertEquals("[Name$0, Name$1, Addr$0, Addr$1, Book$0, Book$0->Name$0->Addr$1, Book$0->Name$1->Addr$0]", m.getLowerBound().toString());
        assertTrue(m.getUpperBound().isEmpty());
    }

    @Test
    public void testAddrBook3cCheck3() {
        String module = "../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3c.als";
        int cmdNum = 3;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);


        assertEquals("[Alias$0, Alias$1, Book$3, Book$3->Alias$1->Alias$0]", m.getLowerBound().toString());
        assertEquals("[UB for field (this/Book <: names)]", m.getUpperBound().toString());
    }

    @Test
    public void testArr2() {
        String module = "../org.alloytools.alloy.eval/models/arepair/arr2.als";
        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum, UBKind.EXACT);


        // this lower bound is fine as Array is a singleton signature already and 1 is an integer
        assertEquals("[Array$0->1]", m.getLowerBound().toString());
        assertEquals("[Element$1, Element$2, Array$0 -> Int[0] -> Element$0, Array$0 -> Int[1] -> Element$0, Array$0 -> Int[2] -> Element$0, Array$0 -> Int[0] -> Element$1, Array$0 -> Int[1] -> Element$1, Array$0 -> Int[2] -> Element$1, Array$0 -> Int[0] -> Element$2, Array$0 -> Int[1] -> Element$2, Array$0 -> Int[2] -> Element$2, Array$0 -> Int[0], Array$0 -> Int[1], Array$0 -> Int[2]]", m.getUpperBound().toString());
    }

}
