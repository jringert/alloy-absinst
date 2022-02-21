package edu.mit.csail.sdg.alloy4whole;

import org.junit.Test;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;


public class MinimizerTest {

    @Test
    public void testAddrBook1aRun() {
        String module = "../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook1a.als";
        int cmdNum = 0;

        Minimizer m = testMin(module, cmdNum);


        System.out.println(m.getLowerBound());
        System.out.println(m.getUpperBound());
    }

    @Test
    public void test() {
        String module = "../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3c.als";
        int cmdNum = 3;

        Minimizer m = testMin(module, cmdNum);


        System.out.println(m.getLowerBound());
        System.out.println(m.getUpperBound());
    }

    private Minimizer testMin(String module, int cmdNum) {
        Module world = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, module);
        Command command = world.getAllCommands().get(cmdNum);
        A4Options options = new A4Options();
        options.solver = A4Options.SatSolver.SAT4J;

        Minimizer m = new Minimizer();
        m.minimize(world.getAllReachableSigs(), command, options);

        return m;
    }

}
