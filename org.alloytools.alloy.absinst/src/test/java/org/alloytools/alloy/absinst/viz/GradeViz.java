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
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4Tuple;

public class GradeViz {

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        String module = "src/test/alloy/small_models/gradeFaulty.als";

        int cmdNum = 0;

        CompModule world = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, module);
        Command command = world.getAllCommands().get(cmdNum);
        A4Options options = new A4Options();
        options.solver = A4Options.SatSolver.SAT4J;

        Minimizer m = new Minimizer();
        m.minimize(world, command, options);

        A4Solution instance = m.getInstOrig();
        HashMap<A4Tuple,String> lower = m.getLowerBoundOriginMap();
        ArrayList<String> upper = m.getUpperBoundNames();

        PrintWriter out = new PrintWriter(System.getProperty("user.dir") + "/src/test/inst/grade.xml", "UTF-8");
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

        //        VizGUI viz = new VizGUI(false, System.getProperty("user.dir") + "/src/test/inst/grade.xml", null);
    }
}
