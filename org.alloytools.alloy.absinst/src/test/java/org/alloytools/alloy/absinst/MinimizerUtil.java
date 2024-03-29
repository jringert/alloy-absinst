package org.alloytools.alloy.absinst;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Options.SatSolver;

public class MinimizerUtil {

    public static SatSolver solver = A4Options.SatSolver.SAT4J;

    public static Minimizer testMin(String module, int cmdNum) {
        return testMin(module, cmdNum, solver);
    }

    public static Minimizer testMin(String module, int cmdNum, SatSolver solver) {
        return testMin(module, cmdNum, UBKind.INSTANCE_OR_NO_UPPER, solver);
    }

    public static Minimizer testMin(String module, int cmdNum, UBKind ub) {
        return testMin(module, cmdNum, ub, solver);
    }

    public static Minimizer testMin(String module, int cmdNum, UBKind ub, SatSolver solver) {
		CompModule world = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, module);
		Command command = world.getAllCommands().get(cmdNum);
		A4Options options = new A4Options();
		options.solver = solver;

		Minimizer m = new Minimizer();
        m.minimize(world, command, options, ub);

		return m;
	}

}
