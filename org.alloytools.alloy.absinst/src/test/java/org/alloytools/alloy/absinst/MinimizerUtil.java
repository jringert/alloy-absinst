package org.alloytools.alloy.absinst;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;

public class MinimizerUtil {
	
	public static Minimizer testMin(String module, int cmdNum) {
		CompModule world = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, module);
		Command command = world.getAllCommands().get(cmdNum);
		A4Options options = new A4Options();
		options.solver = A4Options.SatSolver.SAT4J;

		Minimizer m = new Minimizer();
		m.minimize(world.getAllReachableSigs(), command, options);

		return m;
	}

}
