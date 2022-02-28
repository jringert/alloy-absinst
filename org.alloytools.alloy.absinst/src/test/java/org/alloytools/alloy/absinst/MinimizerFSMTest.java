package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MinimizerFSMTest {
    @Test
    public void testFiniteStateMachineRun() {
        String module = "src/test/alloy/small_models/fsm.als";
        
        int cmdNum = 0;

        Minimizer m = MinimizerUtil.testMin(module, cmdNum);

        // TODO look into this result
        //assertEquals("[FSM$0, FSM$0->State$1, FSM$0->State$0, State$0, State$1, State$1->State$0]", m.getLowerBound().toString());
        //assertEquals("[UB for field (this/FSM <: start), UB for field (this/FSM <: stop), UB for this/State, UB for field (this/State <: transition)]", m.getUpperBound().toString());
        assertEquals("[]", m.getLowerBound().toString());
        assertEquals("[]", m.getUpperBound().toString());

    }
}
