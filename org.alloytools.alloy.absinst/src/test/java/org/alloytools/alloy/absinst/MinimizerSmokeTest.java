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

import org.junit.Test;

/**
 * Test that runs through all models just to see whether sanity checks pass
 *
 */
public class MinimizerSmokeTest {


    @Test
    public void testAllAvailableAlloyFiles() {

        int cmdNum = 0;
        List<Path> alloyFiles = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(Paths.get("src/test/alloy/"))) {
            alloyFiles = stream.map(Path::normalize).filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".als")).collect(Collectors.toList());
        } catch (IOException e) {
            fail("Unable to list files: " + e.toString());
        }

        for (Path p : alloyFiles) {
            if (!p.toString().contains("/iterate/")) {
                System.out.println(p);
                Minimizer m = MinimizerUtil.testMin(p.toString(), cmdNum, UBKind.EXACT);
            }
        }
    }
}
