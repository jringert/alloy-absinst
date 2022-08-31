package org.alloytools.alloy.absint.eval;

import org.junit.jupiter.api.Test;


class EvalMainTest {

    @Test
    void test() {
        EvalMain.main(new String[] {
                                    "models/examples/toys/numbering.als", "first-n", "10", "1", "EXACT", "SAT4J"
        });
    }

    /*
     * models/arepair/arr2.als, first-n, 10, 0, EXACT, MiniSatJNI, abstractInstance,
     * num:, 0, ms:, 28, nop: exception solving, + can be used only between 2
     * expressions of the same arity, or between 2 integer expressions
     * models/arepair/arr2.als, first-n, 10, 0, EXACT, MiniSatJNI, abstractInstance,
     * num:, 1, ms:, 18, nop: exception solving, + can be used only between 2
     * expressions of the same arity, or between 2 integer expressions
     * models/arepair/arr2.als, first-n, 10, 0, EXACT, MiniSatJNI, abstractInstance,
     * num:, 2, ms:, 17, nop: exception solving, + can be used only between 2
     * expressions of the same arity, or between 2 integer expressions
     * models/arepair/arr2.als, first-n, 10, 0, EXACT, MiniSatJNI, abstractInstance,
     * num:, 3, ms:, 42, nop: exception solving, in can be used only between 2
     * expressions of the same arity models/examples/systems/views.als, first-n, 10,
     * 0, EXACT, MiniSatJNI, abstractInstance, num:, 0, ms:, 459917, nop: exception
     * solving, Unknown exception occurred: java.lang.StackOverflowError
     */

    @Test
    void testBugs() {
        EvalMain.main(new String[] {
                                    "models/arepair/arr2.als", "first-n", "1", "0", "EXACT", "SAT4J"
        });
    }



}
