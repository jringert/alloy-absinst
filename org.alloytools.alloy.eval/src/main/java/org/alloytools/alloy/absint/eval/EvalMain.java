package org.alloytools.alloy.absint.eval;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alloytools.alloy.absinst.Minimizer;
import org.alloytools.alloy.absinst.UBKind;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Options.SatSolver;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

public class EvalMain {

    private static String outFolder = "out";

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("parameters: file mode [mode-args] solver");
            System.out.println("example: model.als first-n 10 EXACT SAT4J");
            return;
        }

        String fileName = args[0];
        String mode = args[1];
        String solver = args[args.length - 1];

        Minimizer.DO_SANITY_CHECKS = false;

        switch (mode) {
            case "first-n" :
                int n = Integer.parseInt(args[2]);
                int cmdNum = Integer.parseInt(args[3]);
                UBKind ubKind = UBKind.valueOf(args[4]);
                computeFirstN(args, fileName, n, cmdNum, ubKind, getSolver(solver));
                break;

            default :
                break;
        }

    }

    private static SatSolver getSolver(String solver) {
        if ("SAT4J".equalsIgnoreCase(solver)) {
            return SatSolver.SAT4J;
        }
        if ("MiniSatJNI".equalsIgnoreCase(solver)) {
            return SatSolver.MiniSatJNI;
        }
        if ("MiniSatProverJNI".equalsIgnoreCase(solver)) {
            return SatSolver.MiniSatProverJNI;
        }

        return SatSolver.SAT4J;
    }

    private static void computeFirstN(String[] args, String module, int n, int cmdNum, UBKind ubKind, SatSolver solver) {
        CompModule world = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, module);
        if (world.getAllCommands().isEmpty()) {
            write_report(make_report(args, "nop: no commands"));
            return;
        }
        Command command = world.getAllCommands().get(cmdNum);
        A4Options options = new A4Options();
        options.solver = solver;

        List<A4Solution> solutions = new ArrayList<>();

        A4Solution ans = null;
        for (int i = 0; i < n; i++) {
            long time = System.currentTimeMillis();
            if (ans == null) {
                try {
                    ans = TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, world.getAllReachableSigs(), command, options);
                } catch (Exception e) {
                    time = System.currentTimeMillis() - time;
                    write_report(make_report(args, "concreteInstance", "num:", "" + i, "ms:", "" + time, "nop: exception solving", e.getMessage()));
                    return;
                }
            } else {
                ans = ans.next();
            }
            time = System.currentTimeMillis() - time;

            if (!ans.satisfiable()) {
                break;
            } else {
                write_report(make_report(args, "concreteInstance", "num:", "" + i, "ms:", "" + time, "size:", "" + MeasureInstSize.sizeOf(ans)));
            }
            solutions.add(ans);
        }

        int i = 0;
        for (A4Solution instance : solutions) {
            Minimizer m = new Minimizer();
            long time = System.currentTimeMillis();
            try {
                m.minimize(world, command, instance, options, ubKind);
                time = System.currentTimeMillis() - time;
                write_report(make_report(args, "abstractInstance", "num:", "" + i, "ms:", "" + time, "size:", "" + MeasureInstSize.sizeOf(m), "lb:", m.getLowerBound().toString(), "ub:", m.printUpperBound()));
            } catch (Exception e) {
                time = System.currentTimeMillis() - time;
                write_report(make_report(args, "abstractInstance", "num:", "" + i, "ms:", "" + time, "nop: exception solving", e.getMessage()));
            }
            i++;
        }


    }

    private static String make_report(String[] args, String... info) {
        StringBuffer rep = new StringBuffer();

        rep.append(Arrays.deepToString(args));
        rep.append("\n");
        rep.append(Arrays.deepToString(info));

        return rep.toString();
    }

    private static void write_report(String report) {
        File dir = new File(outFolder);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fName = null;

        while (fName == null || Files.exists(Paths.get(fName))) {
            fName = outFolder + "/" + Math.random() + ".csv";
        }

        try {
            Files.writeString(Paths.get(fName), report, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
