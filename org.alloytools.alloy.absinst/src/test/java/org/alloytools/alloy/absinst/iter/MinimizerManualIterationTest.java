package org.alloytools.alloy.absinst.iter;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alloytools.alloy.absinst.Minimizer;
import org.alloytools.alloy.absinst.TestUtil;
import org.alloytools.alloy.absinst.UBKind;
import org.junit.Test;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Options.SatSolver;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;


public class MinimizerManualIterationTest {

    private static int instanceNum;
    private static int abstractInstanceNum;

    @Test
    public void testSomeARun() {
        String module = "src/test/alloy/basic/someA.als";
        countInstances(module);
        assertEquals(3, instanceNum);
        assertEquals(1, abstractInstanceNum);
    }

    @Test
    public void testAddressBook3a() {
        Minimizer.DO_SANITY_CHECKS = false;
        TestUtil.disableSysOut();

        String module = "../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3a.als";

        countInstances(module);

        TestUtil.restoreSysOut();
        assertEquals(2647, instanceNum);
        assertEquals(1, abstractInstanceNum);
    }

    public static void countInstances(String module) {
        countInstances(module, SatSolver.SAT4J);
    }

    public static void countInstances(String module, SatSolver solver) {
        instanceNum = 0;
        abstractInstanceNum = 0;
        int cmdNum = 0;

        List<A4Solution> instances = new ArrayList<>();

        CompModule world = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, module);
        Command command = world.getAllCommands().get(cmdNum);
        A4Options options = new A4Options();
        options.solver = solver;

        A4Solution ans = TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, world.getAllReachableSigs(), command, options);

        while (ans.satisfiable()) {
            instances.add(ans);
            ans = ans.next();
        }

        instanceNum = instances.size();

        long time = System.currentTimeMillis();
        int instancesAbstracted = 0;

        Set<String> abstractInstances = new HashSet<>();
        for (A4Solution inst : instances) {
            Minimizer m = new Minimizer();
            m.minimize(world, command, inst, options, UBKind.EXACT);
            String absInst = m.getLowerBound().toString() + m.printUpperBound();
            abstractInstances.add(absInst);

            instancesAbstracted++;
            long secPassed = (System.currentTimeMillis() - time) / 1000;
            System.err.println("Abstracted " + instancesAbstracted + " (unique: " + abstractInstances.size() + ") in " + secPassed + "sec.");
        }
        abstractInstanceNum = abstractInstances.size();
    }
}
