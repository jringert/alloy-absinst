package org.alloytools.alloy.absint.eval;

import org.alloytools.alloy.absinst.Minimizer;

import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.translator.A4Solution;

public class MeasureInstSize {

    /**
     * compute size of instance as number of atoms of relevant signatures + number
     * of tuples of their fields
     *
     * @param instance
     * @return size
     */
    public static int sizeOf(A4Solution instance) {
        int atoms = 0;
        int tuples = 0;

        for (Sig s : instance.getAllReachableSigs()) {
            if (Minimizer.isRelevant(s)) {
                atoms += instance.eval(s).size();
                for (Field f : s.getFields()) {
                    tuples += instance.eval(f).size();
                }
            }
        }
        return atoms + tuples;
    }

    /**
     * compute size of abstract instance based on size of lower bound as atoms and
     * tuples + upper bound as constraints on signatures of fields
     *
     * @param absInst
     * @return size
     */
    public static int sizeOf(Minimizer absInst) {
        int lower = absInst.getLowerBound().size();

        int upper = 0;
        switch (absInst.getUbKind()) {
            case EXACT :
                upper = absInst.printUpperBound().split("∌").length - 1;
                if (upper < 0) {
                    upper = 0;
                }
                break;
            case INSTANCE_OR_NO_UPPER :
                upper = absInst.getUpperBound().size();
                break;
            case INSTANCE :
                // fixed UB has no size, TODO might be worth discussion, could be size of original instance?
                upper = 0;
                break;
            case NO_UPPER :
                // never an UB
                upper = 0;
                break;
            default :
                break;
        }

        return lower + upper;
    }

}
