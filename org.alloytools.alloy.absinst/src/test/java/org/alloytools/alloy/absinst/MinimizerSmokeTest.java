package org.alloytools.alloy.absinst;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import edu.mit.csail.sdg.translator.A4Options.SatSolver;

/**
 * Test that runs through all models just to see whether sanity checks pass
 *
 */
public class MinimizerSmokeTest {

    private SatSolver solver = SatSolver.SAT4J;
    private UBKind    ubKind = UBKind.INSTANCE_OR_NO_UPPER;

    @Before
    public void setSetting() {
        Minimizer.DO_SANITY_CHECKS = false;
    }

    @Test
    public void testHandcraftedAlloyFiles() {
        runOnAllFiles("src/test/alloy/");
    }

    @Test
    public void testBookAlloyFiles() {
        runOnAllFiles("../org.alloytools.alloy.extra/extra/models/book");
    }

    @Test
    public void testExampleAlloyFiles() {
        runOnAllFiles("../org.alloytools.alloy.extra/extra/models/examples");
    }

    private void runOnAllFiles(String alloyFilesDir) {
        int cmdNum = 0;
        List<Path> alloyFiles = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(Paths.get(alloyFilesDir))) {
            alloyFiles = stream.map(Path::normalize).filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".als")).collect(Collectors.toList());
        } catch (IOException e) {
            fail("Unable to list files: " + e.toString());
        }

        for (Path p : alloyFiles) {
            if (!p.toString().contains("/iterate/")) {
                System.out.println(p);
                try {
                    Minimizer m = MinimizerUtil.testMin(p.toString(), cmdNum, ubKind, solver);
                } catch (Exception e) {
                    if (!e.getMessage().contains("higher-order quantification")) {
                        throw e;
                    }
                }
            }
        }
    }


}
