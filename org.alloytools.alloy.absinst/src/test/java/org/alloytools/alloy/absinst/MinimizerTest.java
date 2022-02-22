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
        int cmdNum = 4;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);


        assertTrue(!m.getLowerBound().isEmpty());
        assertTrue(m.getUpperBound().isEmpty());
    }


}
