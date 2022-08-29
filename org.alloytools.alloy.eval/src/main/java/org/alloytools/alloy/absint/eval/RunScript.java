package org.alloytools.alloy.absint.eval;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RunScript {

    private static String prefix     = "/usr/bin/time -f \"%M kB; %C\" timeout -k 0s 10m java -jar org.alloytools.alloy.eval.jar ";
    private static String parameters = " first-n 10 EXACT MiniSatJNI";
    private static String suffix     = " 2>> memory.log";

    public static void main(String[] args) {
        List<Path> alloyFiles = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(Paths.get("models"))) {
            alloyFiles = stream.map(Path::normalize).filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".als")).collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println(e);
        }

        for (Path p : alloyFiles) {
            System.out.println(prefix + p.toString().replace("\\", "/") + parameters + suffix);
        }

    }
}
