package org.alloytools.alloy.absinst.viz;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import org.alloytools.alloy.absinst.Minimizer;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.ErrorFatal;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.alloy4viz.VizGUI;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4Tuple;

public class AbsInstVisualizer {

    //This class can be removed, used to explore implementation of AbsWriter
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        String module = args[0];

        int cmdNum = 0;

        CompModule world = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, module);
        Command command = world.getAllCommands().get(cmdNum);
        A4Options options = new A4Options();
        options.solver = A4Options.SatSolver.SAT4J;

        Minimizer m = new Minimizer();
        m.minimize(world.getAllReachableSigs(), world.getAllReachableFacts(), command, options);

        A4Solution instance = m.getInstOrig();
        HashMap<A4Tuple,String> lower = m.getLowerBoundOriginMap();
        ArrayList<String> upper = m.getUpperBoundNames();

        PrintWriter out = new PrintWriter("test.xml", "UTF-8");
        A4Reporter rep = new A4Reporter() {

            @Override
            public void warning(ErrorWarning msg) {
                System.out.println(msg.toString().trim());
                System.out.flush();
            }
        };
        AbsWriter.writeInstance(rep, instance, out, null, null, lower, upper);
        if (out.checkError())
            throw new ErrorFatal("Error writing the solution XML file.");

        VizGUI viz = new VizGUI(false, "src/test/inst/inst1.xml", null);
        viz.loadThemeFile("src/test/inst/inst1_greyYellow.thm");
    }
}
