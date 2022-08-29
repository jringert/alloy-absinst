package org.alloytools.alloy.absinst;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;


public class MinimizerMultipleInstanceTest {

    public static void main(String[] args) throws IOException {
        StringBuffer b = new StringBuffer();
        Files.find(Paths.get("../org.alloytools.alloy.extra/extra/models/book/chapter2/"), 999, (p, bfa) -> bfa.isRegularFile() && p.toString().endsWith(".als")).forEach(f -> b.append(f.toString() + "," + countInstances(f.toString()) + "\n"));
        System.out.println(b);
    }

    @Test
    public void testSomeARun() {
        String module = "src/test/alloy/basic/someA.als";

        assertEquals(3, countInstances(module));
    }

    @Test
    public void testAddressBook3a() {
        String module = "../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3a.als";

        assertEquals(5294, countInstances(module));
    }

    private static int countInstances(String module) {
        int cmdNum = 0;

        Minimizer m = null;
        try {
            m = MinimizerUtil.testMin(module, cmdNum);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return -1;
        }
        Command cmd = m.getCmdOrig();
        List<Sig> sigs = new ArrayList<Sig>(m.getSigsOrig());

        cmd = m.addBounds(cmd, m.getLowerBound(), m.getUpperBound(), sigs);

        A4Options options = new A4Options();
        options.solver = A4Options.SatSolver.SAT4J;

        A4Solution ans = TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, sigs, cmd, options);
        int sols = 0;

        while (ans.satisfiable()) {
            ans = ans.next();
            sols++;
        }
        return sols;
    }
}
