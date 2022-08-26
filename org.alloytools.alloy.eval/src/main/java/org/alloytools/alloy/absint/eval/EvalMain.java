package org.alloytools.alloy.absint.eval;


public class EvalMain {

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("parameters: file mode mode-args solver");
            System.out.println("example: model.als first-n-inst 100 SAT4J");
            System.out.println("example: model.als first-n-abstrInst 100 SAT4J");
        }

        String fileName = args[0];
        String mode = args[1];
        String solver = args[args.length - 1];

        switch (mode) {
            case "first-n-inst" :

                break;

            default :
                break;
        }

    }

}
