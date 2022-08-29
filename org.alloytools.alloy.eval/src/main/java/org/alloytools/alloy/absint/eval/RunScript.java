package org.alloytools.alloy.absint.eval;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;

public class RunScript {

    private static String prefix     = "/usr/bin/time -f \"%M kB; %C\" timeout -k 0s 10m java -jar org.alloytools.alloy.eval.jar ";
    private static String mode       = " first-n 10 ";
    private static String parameters = " EXACT MiniSatJNI";
    private static String suffix     = " 2>> memory.log";

    public static void main(String[] args) {
        List<Path> alloyFiles = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(Paths.get("models"))) {
            alloyFiles = stream.map(Path::normalize).filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".als")).collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println(e);
        }

        for (Path p : alloyFiles) {
            int numCmds = getNumCommands(p);
            for (int i = 0; i < numCmds; i++) {
                System.out.println(prefix + p.toString().replace("\\", "/") + mode + i + parameters + suffix);
            }
        }

    }

    private static int getNumCommands(Path p) {
        CompModule world = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, p.toString());
        return world.getAllCommands().size();
    }
}
