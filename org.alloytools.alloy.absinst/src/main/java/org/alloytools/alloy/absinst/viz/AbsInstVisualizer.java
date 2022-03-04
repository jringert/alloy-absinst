package org.alloytools.alloy.absinst.viz;

import edu.mit.csail.sdg.alloy4viz.VizGUI;

public class AbsInstVisualizer {

    public static void main(String[] args) {
        VizGUI viz = new VizGUI(false, "src/test/inst/inst1.xml", null);
        viz.loadThemeFile("src/test/inst/inst1_greyYellow.thm");

    }
}
