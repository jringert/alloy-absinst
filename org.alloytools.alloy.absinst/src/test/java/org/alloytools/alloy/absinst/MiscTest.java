package org.alloytools.alloy.absinst;

import java.util.Arrays;

import org.junit.Test;

import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Decl;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprQt;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.PrimSig;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;

public class MiscTest {

    @Test
    public void testApi() {
        Sig sigA = new PrimSig("A");

        ExprVar vA1 = ExprVar.make(null, "A1", Sig.UNIV.type());
        ExprVar vA2 = ExprVar.make(null, "A2", Sig.UNIV.type());

        Decl decl = new Decl(null, new Pos("disjoint", 0, 0), null, null, Arrays.asList(vA1, vA2), Sig.UNIV.one());

        Expr lowerBound = sigA.equal(vA1.plus(vA2));

        lowerBound = ExprQt.Op.SOME.make(null, null, Arrays.asList(decl), lowerBound);
        System.out.println(lowerBound);
    }

    @Test
    public void testParse() {
        CompModule world = CompUtil.parseEverything_fromString(null, "sig A {}");
        Expr lowerBound = CompUtil.parseOneExpression_fromString(world, "some disj A1, A2 : univ | A = A1 + A2");
        System.out.println(lowerBound);
    }
}
