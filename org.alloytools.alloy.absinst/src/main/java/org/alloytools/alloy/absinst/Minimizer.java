package org.alloytools.alloy.absinst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Attr;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Decl;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprBinary;
import edu.mit.csail.sdg.ast.ExprConstant;
import edu.mit.csail.sdg.ast.ExprList;
import edu.mit.csail.sdg.ast.ExprQt;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.Sig.PrimSig;
import edu.mit.csail.sdg.ast.Type.ProductType;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4Tuple;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

/**
 * Idea is to: - take an instance as lower and upper bounds - reduce lower bound
 * until check() fails - increase upper bound until check() fails
 *
 * check is checking that negation of command has no instance -- this could
 * break if we break the bounds and make the problem unsat check should also
 * check that command has a solution (ideally the one we started from)
 *
 * is it safer to add these as constraints? rather than adding them as bounds?
 *
 * Issues expected: - some constraints are realized as bounds, e.g., one sig. -
 * ordering might rely on some fixed size of the ordered relation? -
 *
 * @author mdd
 *
 */
public class Minimizer {

    /**
     * run some sanity checks during minimization, e.g., check for satisfiability of
     * problem with original bounds
     */
    public static boolean              DO_SANITY_CHECKS = true;

    /**
     * signatures of the original problem
     */
    private ConstList<Sig>             sigsOrig;
    /**
     * original facts
     */
    private Expr                       factsOrig;
    /**
     * original command
     */
    private Command                    cmdOrig;
    /**
     * original options
     */
    private A4Options                  optOrig;

    private List<BoundElement>         instance       = new ArrayList<>();
    /**
     * current lower bound (will shrink over runs)
     */
    private List<BoundElement>         lower          = new ArrayList<>();
    /**
     * current upper bound (will shrink over runs)
     */
    private List<BoundElement>         upper          = new ArrayList<>();
    /**
     * initial upper bound (all possible atoms and tuples not in the instance)
     */
    private List<BoundElement>         upperOrig      = new ArrayList<>();
    /**
     * the kind of upper bound we use (different interpretation of bound elements in
     * upper)
     */
    private UBKind                     ubKind         = UBKind.EXACT;

    protected Map<String,PrimSig>      loneSig        = new LinkedHashMap<>();

    protected Map<String,Sig>     enumVal          = new LinkedHashMap<>();

    private A4Solution                 instOrig;

    protected class BoundElement {

        Field   f;
        Sig     s;
        A4Tuple t;
        String  atomName;
        Expr    expr;

        boolean isAtom() {
            if (atomName != null) {
                return true;
            }
            return (t != null && t.arity() == 1);
        }

        String atomName() {
            if (atomName != null) {
                return atomName;
            }
            return t.atom(0);
        }

        @Override
        public String toString() {
            if (atomName != null) {
                return atomName;
            }
            if (t != null) {
                return t.toString();
            } else if (expr != null) {
                return expr.toString();
            } else if (s != null) {
                return "UB for " + s.toString();
            } else {
                return "UB for " + f.toString();
            }
        }

    }

    private String boundMsg;
    public A4Reporter rep = new A4Reporter() {


                           // For example, here we choose to display each "warning" by printing
                           // it to System.out
                           @Override
                           public void warning(ErrorWarning msg) {
                               System.out.print("Relevance Warning:\n" + (msg.toString().trim()) + "\n\n");
                               System.out.flush();
                           }

                           @Override
                           public void bound(String msg) {
                               if (boundMsg == null || boundMsg.isBlank()) {
                                   boundMsg = msg;
                               } else {
                                   boundMsg += msg;
                               }
                           }

                           //        @Override
                           //        public void scope(String msg) {
                           //            System.out.println(msg);
                           //        }
                       };

    public class DdminAbsInstBounds extends AbstractDdmin<BoundElement> {

        private boolean low = true;

        /**
         * Minimizes bounds, either upper or lower
         *
         * @param low
         */
        public DdminAbsInstBounds(boolean low) {
            this.low = low;
        }

        @Override
        protected boolean check(List<BoundElement> candidate) {
            // check negation of command for instances
            List<Sig> cmdSigs = new ArrayList<>(sigsOrig);
            Command cmdWithBounds = null;
            Command cmdSanity = null;
            if (low) {
                cmdWithBounds = addBounds(negatePred(cmdOrig), candidate, upper, cmdSigs);
                if (DO_SANITY_CHECKS) {
                    cmdSanity = addBounds(cmdOrig, candidate, upper, new ArrayList<>(sigsOrig));
                }
            } else {
                cmdWithBounds = addBounds(negatePred(cmdOrig), lower, candidate, cmdSigs);
                if (DO_SANITY_CHECKS) {
                    cmdSanity = addBounds(cmdOrig, lower, candidate, new ArrayList<>(sigsOrig));
                }
            }

            if (DO_SANITY_CHECKS) {
                A4Solution ansSanity = TranslateAlloyToKodkod.execute_command(rep, cmdSigs, cmdSanity, optOrig);
                if (!ansSanity.satisfiable()) {
                    throw new RuntimeException("Unexpected UNSAT result of problem with new bounds that should include the original instance.");
                }
            }
            A4Solution ans = TranslateAlloyToKodkod.execute_command(rep, cmdSigs, cmdWithBounds, optOrig);
            return !ans.satisfiable();
        }

        /**
         * negates the original predicate of the run/check command (without negating
         * facts)
         *
         * @param cmdOrig
         * @return
         */
        private Command negatePred(Command cmdOrig) {
            Expr negPred = null;
            List<Expr> facts = null;
            if (factsOrig.equals(ExprConstant.TRUE)) {
                facts = new ArrayList<>();
            } else if (factsOrig instanceof ExprList) {
                facts = new ArrayList<Expr>(((ExprList) factsOrig).args);
            } else {
                throw new RuntimeException("Case of facts not handled: " + factsOrig);
            }
            switch (cmdOrig.formula.getClass().getSimpleName()) {
                case "ExprList" :
                    List<Expr> pred = new ArrayList<Expr>(((ExprList) cmdOrig.formula).args);
                    pred.removeAll(facts);
                    if (pred.size() == 0) {
                        negPred = ExprConstant.FALSE;
                    } else if (pred.size() == 1) {
                        negPred = pred.get(0).not();
                    } else {
                        negPred = ExprList.make(null, null, ExprList.Op.AND, pred).not();
                    }
                    break;
                default :
                    throw new RuntimeException("Case of formula in command not handled: " + cmdOrig.formula);
            }
            return cmdOrig.change(factsOrig.and(negPred));
        }
    }

    public static void main(String[] args) {
        Module world = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, "../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3c.als");
        Command command = world.getAllCommands().get(world.getAllCommands().size() - 1);
        A4Options options = new A4Options();
        options.solver = A4Options.SatSolver.SAT4J;

        Minimizer m = new Minimizer();
        m.minimize(world, command, options);

        printBounds(m);
    }

    private static void printBounds(Minimizer m) {
        System.out.println("LB = " + m.getLowerBound());
        System.out.println("UB = " + m.getUpperBound());
    }

    /**
     * minimization of bounds (uses legacy UBKind = INSTANCE_OR_NO_UPPER)
     *
     * @param world
     * @param cmd
     * @param opt
     */
    public void minimize(Module world, Command cmd, A4Options opt) {
        minimize(world, cmd, opt, UBKind.INSTANCE_OR_NO_UPPER);
    }

    /**
     * minimization of an instance (uses legacy UBKind = INSTANCE_OR_NO_UPPER)
     *
     * @param world
     * @param cmd
     * @param ans
     * @param opt
     */
    public void minimize(Module world, Command cmd, A4Solution ans, A4Options opt) {
        minimize(world, cmd, ans, opt, UBKind.INSTANCE_OR_NO_UPPER);
    }

    public void minimize(Module world, Command cmd, A4Options opt, UBKind ub) {
        A4Solution ans = TranslateAlloyToKodkod.execute_command(rep, world.getAllReachableSigs(), cmd, opt);
        minimize(world, cmd, ans, opt, ub);
    }


    public void minimize(Module world, Command cmd, A4Solution ans, A4Options opt, UBKind ub) {
        this.sigsOrig = world.getAllReachableSigs();
        this.factsOrig = world.getAllReachableFacts();
        this.cmdOrig = cmd;
        this.optOrig = opt;
        this.boundMsg = "";
        this.ubKind = ub;
        this.instOrig = ans;

        // check empty bounds
        if (isTrue(cmd)) {
            upper = new ArrayList<>();
            lower = new ArrayList<>();
            return;
        }

        // run once to get bounds written by reporter
        TranslateAlloyToKodkod.execute_command(rep, this.sigsOrig, cmd, opt);

        if (!ans.satisfiable()) {
            return;
        }

        initBounds(ans);

        if (DO_SANITY_CHECKS) {
            // sanity check that within given bounds we have some valid instance
            ArrayList<Sig> sigsSanity = new ArrayList<>(this.sigsOrig);
            Command cmdSanity = addBounds(cmd, lower, upper, sigsSanity);
            A4Solution ansSanity = TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, sigsSanity, cmdSanity, opt);
            if (!ansSanity.satisfiable()) {
                throw new RuntimeException("Problem unsat with original bounds.");
            }
        }
        if (DO_SANITY_CHECKS || UBKind.NO_UPPER.equals(ub)) {
            // sanity check that within given bounds we DON'T have any invalid instance
            // also check for NO_UPPER as that kind is incomplete
            DdminAbsInstBounds ddmin = new DdminAbsInstBounds(false);
            boolean check = ddmin.check(upper); // all instances in bounds satisfy command
            if (!check) {
                if (UBKind.NO_UPPER.equals(ub)) {
                    throw new RuntimeException("No abstract instance exists for UB kind NO_UPPER (this bound kind is incomplete by nature).");
                } else {
                    throw new RuntimeException("Instances in initial bounds that violate the command.");
                }
            }
        }

        minLowerUpper(ub);

    }

    private boolean isTrue(Command cmd) {
        Expr negPred = null;
        List<Expr> facts = null;
        if (factsOrig.equals(ExprConstant.TRUE)) {
            facts = new ArrayList<>();
        } else if (factsOrig instanceof ExprList) {
            facts = new ArrayList<Expr>(((ExprList) factsOrig).args);
        } else {
            return false;
        }
        switch (cmd.formula.getClass().getSimpleName()) {
            case "ExprList" :
                List<Expr> pred = new ArrayList<Expr>(((ExprList) cmd.formula).args);
                pred.removeAll(facts);
                if (pred.size() == 0) {
                    return true;
                }
                break;
            default :
                return false;
        }
        return false;
    }

    private void minLowerUpper(UBKind ub) {
        boolean rerun = true;
        DdminAbsInstBounds min;
        while (rerun) {
            rerun = false;

            // don't minimize instance or empty bounds
            if (!UBKind.INSTANCE.equals(ub) && !UBKind.NO_UPPER.equals(ub)) {
                min = new DdminAbsInstBounds(false);
                List<BoundElement> newUpper = min.minimize(upper);
                if (upper.size() > newUpper.size()) {
                    rerun = true;
                    upper = newUpper;
                }
            }

            min = new DdminAbsInstBounds(true);
            List<BoundElement> newLower = min.minimize(lower);
            if (lower.size() > newLower.size()) {
                rerun = true;
                lower = newLower;
            } else {
                // no change in lower; this means upper is still valid
                rerun = false;
            }
            //printBounds(this);
        }
    }

    /**
     *
     * upper bound will never go below instance no S[n+1]..no S[scope]
     *
     *
     * @param ans
     */
    private void initBounds(A4Solution ans) {
        // lower bounds are exact tuples
        for (Sig s : ans.getAllReachableSigs()) {
            if (isRelevant(s)) {
                for (A4Tuple t : ans.eval(s)) {
                    if (t != null) {
                        BoundElement be = new BoundElement();
                        be.s = s;
                        be.atomName = t.atom(0);
                        be.t = t;
                        // we only do it for the actual signature otherwise
                        // this would lead to creating a tuple twice or multiple times if it shows
                        // up multiple times across the inheritance hierarchy
                        if (instanceOf(be, s)) {
                            lower.add(be);
                            PrimSig atom = loneSig.get(be.atomName());
                            if (atom == null) {
                                // FIXME this breaks because Alloy will name the next atom again as sig$0
                                // even if the lone sig for it already exists

                                // FIXME check whether this works for the second run where the atoms
                                // are created from lone sigs or whether we need to modify names here
                                atom = new Sig.PrimSig(be.atomName(), (PrimSig) be.s, Attr.LONE);
                                loneSig.put(be.atomName(), atom);
                            }
                            be.expr = atom;
                        }
                    }
                }
                for (Field f : s.getFields()) {
                    for (A4Tuple t : ans.eval(f)) {
                        if (t != null) {
                            BoundElement e = new BoundElement();
                            e.f = f;
                            // TODO check that this always works to look up atoms by name
                            // TODO consider creating tuples based on new signatures here
                            // doesn't work in predicate based approach?
                            e.t = t;
                            lower.add(e);
                        }
                    }
                }
            } else if (isEnumConstant(s)) {
                enumVal.put(s.label, s);
            }
        }

        // copy lower bounds as instance
        instance.addAll(lower);

        if (UBKind.INSTANCE_OR_NO_UPPER.equals(ubKind) || UBKind.INSTANCE.equals(ubKind)) {
            // upper bounds are either fixed to instance or open
            for (Sig s : ans.getAllReachableSigs()) {
                if (isRelevant(s)) {
                    BoundElement es = new BoundElement();
                    es.s = s;
                    upper.add(es);
                    for (Field f : s.getFields()) {
                        BoundElement ef = new BoundElement();
                        ef.f = f;
                        upper.add(ef);
                    }
                }
            }
        } else if (UBKind.NO_UPPER.equals(ubKind)) {
            upper.clear(); // not really necessary, more of a symbolic act
        } else if (UBKind.EXACT.equals(ubKind)) {
            for (Sig s : ans.getAllReachableSigs()) {
                if (isRelevant(s) && s.isAbstract == null && s instanceof PrimSig) {
                    int atomNum = 0;
                    for (String atom : getAtoms(s, boundMsg)) {
                        BoundElement es = new BoundElement();
                        es.s = s;
                        es.atomName = sigLabelToAtomName(s.label) + "$" + atomNum++;
                        if (!isInInstance(es)) {
                            upper.add(es);
                            // add as lone sig in case it is needed later
                            if (loneSig.get(es.atomName()) == null) {
                                PrimSig atomOfSig = new Sig.PrimSig(es.atomName(), (PrimSig) es.s, Attr.LONE);
                                loneSig.put(es.atomName(), atomOfSig);
                                es.expr = atomOfSig;
                            } else {
                                es.expr = loneSig.get(es.atomName());
                            }
                            //System.out.println("added UB lone atom " + es.atomName);
                        } else {
                            //System.out.println("ignoring UB atom " + es.atomName + " from instance");
                        }
                    }
                }
            }
            // iterate a second time for all fields (now all atoms exist
            for (Sig s : ans.getAllReachableSigs()) {
                if (isRelevant(s)) {
                    // build the whole UB also including tuples
                    for (Field f : s.getFields()) {
                        for (ProductType pt : f.type()) {
                            for (Expr tuple : generateTuples(pt)) {
                                BoundElement ef = new BoundElement();
                                ef.f = f;
                                ef.expr = tuple;
                                if (!isInInstance(ef)) {
                                    upper.add(ef);
                                }
                            }
                        }
                    }
                }
            }
        }
        // remember initial value of upper bound
        upperOrig = new ArrayList<Minimizer.BoundElement>(upper);
    }

    private List<Expr> generateTuples(ProductType pt) {
        List<Expr> exprs = new ArrayList<>();
        for (int i = 0; i < pt.arity(); i++) {
            List<Expr> exprsExt = new ArrayList<>();
            PrimSig sig = pt.get(i);
            List<Expr> atoms = getPossibleAtoms(sig);
            for (Expr atom : atoms) {
                for (Expr tuple : exprs) {
                    exprsExt.add(product(tuple, atom));
                }
                if (exprs.isEmpty()) {
                    exprsExt.add(atom);
                }
            }
            exprs = exprsExt;
        }
        return exprs;
    }

    /**
     * retrieves the possible atoms of this sig for the special case here where
     * atoms are represented by lone or one sigs
     *
     * @param sig
     * @return
     */
    private List<Expr> getPossibleAtoms(PrimSig sig) {
        List<Expr> atoms = new ArrayList<>();
        if (sig == Sig.UNIV) {
            throw new RuntimeException("UNIV atoms not calculated yet");
        }
        for (PrimSig child : sig.children()) {
            atoms.addAll(getPossibleAtoms(child));
        }
        if (sig.children().isEmpty()) {
            if (sig.isLone != null || sig.isOne != null) {
                atoms.add(sig);
            } else if ("seq/Int".equals(sig.label)) {
                for (ExprVar exprVar : instOrig.getAllAtoms()) {
                    if (exprVar.type().is_int()) {
                        int i = Integer.parseInt(exprVar.label);
                        atoms.add(ExprConstant.makeNUMBER(i));
                    }
                }
            } else {
                throw new RuntimeException("sig " + sig.label + " not handled yet");
            }
        }

        return atoms;
    }

    /**
     * translates a signature label into an atom name (prefix of it) --- current
     * implementation removes prefix "this/"
     *
     * @param label
     * @return
     */
    private String sigLabelToAtomName(String label) {
        String aName = label.replaceAll("this/", "");
        return aName;
    }

    /**
     * determine whether a bound element is contained in the original instance
     *
     * @param es
     * @return
     */
    private boolean isInInstance(BoundElement es) {
        if (es.isAtom()) {
            for (BoundElement e : instance) {
                if (e.isAtom() && e.atomName.equals(es.atomName)) {
                    return true;
                }
            }
        } else {
            for (BoundElement e : instance) {
                if (!e.isAtom() && e.f.label.equals(es.f.label)) {
                    if (es.t != null && sameTuple(e.t, es.t)) {
                        return true;
                    } else if (es.expr != null && sameTuple(e.t, es.expr)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean sameTuple(A4Tuple t, Expr expr) {
        if (expr instanceof ExprBinary && ExprBinary.Op.ARROW.equals(((ExprBinary) expr).op)) {
            LinkedList<Sig> atomsInExpr = atomsInProduct(expr);

            if (t.arity() != atomsInExpr.size()) {
                return false;
            }
            for (int i = 0; i < t.arity(); i++) {
                if (!t.atom(i).equals(atomsInExpr.get(i).label)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * given a product expression extract all atoms
     *
     * @param expr
     * @return
     */
    private LinkedList<Sig> atomsInProduct(Expr expr) {
        LinkedList<Sig> atomsInExpr = new LinkedList<>();
        while (expr instanceof ExprBinary) {
            ExprBinary exprB = (ExprBinary) expr;
            atomsInExpr.addFirst((Sig) exprB.right);
            expr = exprB.left;
        }
        atomsInExpr.addFirst((Sig) expr);
        return atomsInExpr;
    }

    /**
     * check atom name correspondence in tuples
     *
     * @param t
     * @param t2
     * @return
     */
    private boolean sameTuple(A4Tuple t1, A4Tuple t2) {
        if (t1.arity() != t2.arity()) {
            return false;
        }
        for (int i = 0; i < t1.arity(); i++) {
            if (!t1.atom(i).equals(t2.atom(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * extracts atom names from the bounds message of the reporter
     *
     * @param s
     * @param bound
     * @return array of atom names that are the bound of the given signature
     */
    private String[] getAtoms(Sig s, String bound) {
        for (String line : bound.split("\n")) {
            String ini1 = "Sig " + s.label + " in ";
            String ini2 = "Sig " + s.label + " == ";
            if (line.startsWith(ini1) || line.startsWith(ini2)) {
                line = line.substring(ini1.length());
                line = line.replaceAll("\\[", "");
                line = line.replaceAll("\\]", "");
                return line.split(", ");
            }
        }
        throw new RuntimeException("Unable to retrieve atoms for " + s.label + " from bound: " + bound);
    }

    /**
     * check whether this atom is a direct instance of the signature (false if it is
     * an instance of a sub-signature)
     *
     * @param e
     * @param s
     * @return
     */
    private boolean instanceOf(BoundElement e, Sig s) {
        return instanceOf(e.atomName(), s);
    }

    /**
     * check whether this atom is a direct instance of the signature (false if it is
     * an instance of a sub-signature)
     *
     * @param e
     * @param s
     * @return
     */
    private boolean instanceOf(String atomName, Sig s) {
        return atomName.startsWith(s.label.replaceAll("this/", "") + "$");
    }

    /**
     * checks relevance of signatures for computing bounds
     *
     * bounds should be ignored for signatures where this returns false, e.g.,
     * built-in, meta, or enum sigs
     *
     * @param s
     * @return true if sig is relevant
     */
    public static boolean isRelevant(Sig s) {
        if (s.builtin) {
            return false;
        }
        if (s.isMeta != null) {
            return false;
        }
        if (s.isEnum != null) {
            return false;
        }
        if (s.isSubset != null) {
            return false;
        }
        if (isEnumConstant(s)) {
            return false;
        }
        // TODO this is just a guess
        if (s.toString().endsWith("/Ord")) {
            return false;
        }

        return true;
    }

    private static boolean isEnumConstant(Expr e) {
        if (e instanceof Sig) {
            Sig s = (Sig) e;
            if (s.isSubsig != null && s instanceof PrimSig) {
                if (((PrimSig) s).parent != null && ((PrimSig) s).parent.isEnum != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * adds constraints for upper and lower bounds to the command
     *
     * @param cmd command to be updated with constraints
     * @param lower lower bound to set
     * @param upper upper bound to set (interpreted based on ubKind boud kind)
     * @param cmdSigs additional signatures that are contributed to express the
     *            constraints
     * @return updated command with constraints for upper and lower bounds
     */
    public Command addBounds(Command cmd, List<BoundElement> lower, List<BoundElement> upper, List<Sig> cmdSigs) {
        Expr lowerBound = null;
        Expr upperBound = null;

        // lower bounds require things to be there
        for (BoundElement e : lower) {
            if (e.isAtom()) {
                // atoms only added as singleton sigs
                Sig s = loneSig.get(e.atomName());
                lowerBound = and(lowerBound, s.one());
                if (!cmdSigs.contains(s)) {
                    cmdSigs.add(s);
                }
            } else {
                // fields added as constraints
                Expr tuple = null;
                boolean sigMissingAndTupleInvalid = false;
                for (int i = 0; i < e.t.arity(); i++) {
                    Expr s = retrieveAtomExpr(e.t.atom(i));
                    tuple = product(tuple, s);
                    // reject tuple if sig is missing
                    // INFO this is a deliberate decision as there will be a larger set "lower" for all possible tuples (larges is the instance itself)
                    if (!isEnumConstant(s) && !hasAtom(lower, e.t.atom(i))) {
                        sigMissingAndTupleInvalid = true;
                        // stop handling this tuple (inner loop)
                        break;
                    }
                }
                if (!sigMissingAndTupleInvalid && tuple != null) {
                    Expr bound = tuple.in(e.f);
                    lowerBound = and(lowerBound, bound);
                }
            }
        }

        // upper bounds put a restriction on max from the instance
        if (UBKind.INSTANCE.equals(ubKind) || UBKind.INSTANCE_OR_NO_UPPER.equals(ubKind)) {
            for (BoundElement e : upper) {
                if (e.s != null) {
                    upperBound = addSigUBFromInstance(cmdSigs, upperBound, e);
                } else {
                    upperBound = addFieldUBFromInstance(cmdSigs, upperBound, e);
                }
            }
        } else if (UBKind.EXACT.equals(ubKind)) {
            // add lones for atoms in instance not in lower bound
            for (BoundElement e : instance) {
                if (e.isAtom()) {
                    PrimSig s = loneSig.get(e.atomName());
                    if (!cmdSigs.contains(s)) {
                        cmdSigs.add(s);
                    }
                }
            }

            // constrain to lower + inst + upper
            List<BoundElement> l = new ArrayList<>(upperOrig);
            l.removeAll(upper);
            for (Sig s : sigsOrig) {
                // for every relevant sig s collect max atoms
                if (isRelevant(s)) {
                    Expr max = null;
                    // iterate over existing children and atom sigs of s (from original
                    // module or lower bound)
                    for (Sig child : cmdSigs) {
                        if (child instanceof PrimSig) {
                            if (((PrimSig) child).parent == s) {
                                max = plus(max, child);
                            }
                        }
                    }
                    // add also upper bound elements to max and add them as lone sigs
                    for (BoundElement e : l) {
                        if (e.isAtom() && e.s == s) {
                            Sig atom = loneSig.get(e.atomName);
                            if (!cmdSigs.contains(atom)) {
                                cmdSigs.add(atom);
                            }
                            max = plus(max, atom);
                        }
                    }
                    // set max of sig
                    if (max == null) {
                        upperBound = and(upperBound, s.no());
                    } else {
                        upperBound = and(upperBound, s.equal(max));
                    }
                }
            }

            // TODO do something similar for fields
            for (Sig s : sigsOrig) {
                // for every relevant sig s collect max atoms
                if (isRelevant(s)) {
                    for (Field f : s.getFields()) {
                        Expr max = null;

                        // add tuples that are in instance (including lower bound)
                        for (BoundElement e : instance) {
                            if (!e.isAtom() && e.f == f) {
                                Expr tuple = null;
                                boolean sigMissingAndTupleInvalid = false;
                                for (int i = 0; i < e.t.arity(); i++) {
                                    Expr atom = retrieveAtomExpr(e.t.atom(i));
                                    tuple = product(tuple, atom);
                                    // reject tuple if sig is missing
                                    // INFO this is a deliberate decision as we don't want to restrict tuples in addition to atoms
                                    if (!hasSigForAtom(cmdSigs, e.t.atom(i))) {
                                        sigMissingAndTupleInvalid = true;
                                        // stop handling this tuple (inner loop)
                                        break;
                                    }
                                }
                                if (!sigMissingAndTupleInvalid && tuple != null) {
                                    max = plus(max, tuple);
                                }
                            }
                        }

                        // add also upper bound elements to max
                        for (BoundElement e : l) {
                            if (!e.isAtom() && e.f == f) {
                                if (mightHaveAtoms(e.expr, cmdSigs)) {
                                    max = plus(max, e.expr);
                                }
                            }
                        }
                        // set max of field
                        if (max == null) {
                            upperBound = and(upperBound, f.no());
                        } else {
                            upperBound = and(upperBound, f.in(max));
                        }
                    }
                }
            }

        }

        Expr f = cmd.formula;
        if (lowerBound != null) {
            f = f.and(lowerBound);
            //System.out.println("LB for check: one sigs for atoms of " + atomsOf(lower) + " and " + lowerBound);
        } else {
            //System.out.println("LB for check: one sigs for atoms of " + atomsOf(lower));
        }
        //System.out.println("UB for check: " + upperBound);
        if (upperBound != null) {
            f = f.and(upperBound);
        }
        return cmd.change(f);
    }

    /**
     * checks whether the cmdSigs allow for all atoms in the product expression to
     * be instantiated
     *
     * @param expr
     * @param cmdSigs
     */
    private boolean mightHaveAtoms(Expr expr, List<Sig> cmdSigs) {
        return cmdSigs.containsAll(atomsInProduct(expr));
    }

    /**
     * union of e1 and e2 robust to nulls
     *
     * @param max
     * @param s
     */
    private Expr plus(Expr e1, Expr e2) {
        if (e1 == null) {
            return e2;
        }
        if (e2 == null) {
            return e1;
        }
        return e1.plus(e2);
    }

    /**
     * conjunction of e1 and e2 robust to nulls
     *
     * @param e1
     * @param e2
     */
    private Expr and(Expr e1, Expr e2) {
        if (e1 == null) {
            return e2;
        }
        if (e2 == null) {
            return e1;
        }
        return e1.and(e2);
    }

    /**
     * product of e1 and e2 robust to nulls
     *
     * @param e1
     * @param e2
     */
    private Expr product(Expr e1, Expr e2) {
        if (e1 == null) {
            return e2;
        }
        if (e2 == null) {
            return e1;
        }
        return e1.product(e2);
    }


    private Expr addFieldUBFromInstance(List<Sig> cmdSigs, Expr upperBound, BoundElement e) {
        Expr atMost = null;
        // find instance elements for field (from upper bound)
        for (BoundElement ei : instance) {
            if (ei.f == e.f) {
                Expr tuple = null;
                for (int i = 0; i < ei.t.arity(); i++) {
                    // construct tuple based on atoms
                    String atomName = ei.t.atom(i);
                    // check if one atom is used
                    Expr atom = loneSig.get(atomName);
                    // atom not found in sigs or one sig not included
                    if (atom == null || !cmdSigs.contains(atom)) {
                        atom = retrieveAtomExpr(atomName);
                    }
                    // add lone sig if needed
                    if (atom instanceof PrimSig) {
                        if (!cmdSigs.contains(atom)) {
                            cmdSigs.add((Sig) atom);
                        }
                    }
                    if (tuple == null) {
                        tuple = atom;
                    } else {
                        tuple = tuple.product(atom);
                    }
                }
                if (atMost == null) {
                    atMost = tuple;
                } else {
                    atMost = atMost.plus(tuple);
                }
            }

        }
        Expr bound = null;
        if (atMost == null) {
            bound = e.f.no();
        } else {
            bound = e.f.in(atMost);
        }
        if (upperBound == null) {
            upperBound = bound;
        } else {
            upperBound = upperBound.and(bound);
        }
        return upperBound;
    }

    private Expr addSigUBFromInstance(List<Sig> cmdSigs, Expr upperBound, BoundElement e) {
        // we need to bound the signature to the upper bound of the instance
        Expr atMost = null;
        for (BoundElement ie : instance) {
            // only care about this signature
            if (ie.s == e.s && instanceOf(ie, e.s)) {
                PrimSig s = loneSig.get(ie.atomName());
                if (!cmdSigs.contains(s)) {
                    cmdSigs.add(s);
                }
                atMost = plus(atMost, s);
            }
        }
        upperBound = boundSigToAtMostExpr(upperBound, e.s, atMost);
        return upperBound;
    }

    /**
     * extends upperBound with a conjunct where S in subsigs of S + atMost
     *
     * @param upperBound existing UB constraints (initialized if null)
     * @param s signature to add bound for
     * @param atMost union of elements in UB
     * @return
     */
    private Expr boundSigToAtMostExpr(Expr upperBound, Sig s, Expr atMost) {
        // collect union of subsignatures of s
        Expr subs = null;
        for (Sig ch : sigsOrig) {
            if (ch instanceof PrimSig) {
                PrimSig pch = (PrimSig) ch;
                if (pch.parent == s) {
                    // no need to look for further children as inheritance takes care of that
                    if (subs == null) {
                        subs = pch;
                    } else {
                        subs = subs.plus(pch);
                    }
                }
            }
        }

        Expr bound = null;
        // there might not have been an atom in the instance so upper bound is empty set
        if (atMost == null && subs == null) {
            // e.s doesn't have sub signatures and there is no atom for e.s
            // we set s = {}
            bound = s.no();
        } else if (atMost == null) {
            // there is no atom in e.s, but maybe in its subsigs
            // we set s = children of s
            bound = s.equal(subs);
        } else {
            // we set s in (atoms + children sigs)
            bound = s.in(atMost.plus(subs));
        }
        upperBound = and(upperBound, bound);
        return upperBound;
    }

    /**
     * adds bounds as a predicate only (does not add any signatures to cmdSigs)
     *
     * @param cmd
     * @param lower
     * @param upper
     * @param cmdSigs
     * @return a command with bounds added to run predicate
     */
    public Command addBoundsPred(Command cmd, List<BoundElement> lower, List<BoundElement> upper, List<Sig> cmdSigs) {
        Expr lowerBound = null;
        Expr upperBound = null;

        lowerBound = lowerBoundsPred(lower, lowerBound);

        // upper bounds put a restriction on max from the instance
        for (BoundElement e : upper) {
            if (e.s != null) {
                upperBound = addSigUBFromInstance(cmdSigs, upperBound, e);
            } else {
                upperBound = addFieldUBFromInstance(cmdSigs, upperBound, e);
            }
        }

        Expr f = cmd.formula;
        if (lowerBound != null) {
            f = f.and(lowerBound);
        }
        //System.out.println("LB for check: " + lowerBound);
        if (upperBound != null) {
            f = f.and(upperBound);
        }
        //System.out.println("UB for check: " + upperBound);
        return cmd.change(f);
    }

    private Expr lowerBoundsPred(List<BoundElement> lower, Expr lowerBound) {
        Map<String,ExprVar> vars = new LinkedHashMap<>();

        // add constraints for atoms to be in sigs
        for (BoundElement e : lower) {
            if (e.isAtom()) {
                ExprVar v = ExprVar.make(null, e.atomName(), Sig.UNIV.type());
                vars.put(e.atomName(), v);
                Expr bound = v.in(e.s);
                if (lowerBound == null) {
                    lowerBound = bound;
                } else {
                    lowerBound = lowerBound.and(bound);
                }
            }
        }

        if (vars.isEmpty()) {
            return ExprConstant.TRUE;
        }

        // add constraints for fields
        for (BoundElement e : lower) {
            if (!e.isAtom()) {
                // fields added as constraints
                Expr tuple = null;
                boolean sigMissingAndTupleInvalid = false;
                for (int i = 0; i < e.t.arity(); i++) {
                    String atom = e.t.atom(i);
                    Expr s = vars.get(atom);
                    if (s == null) {
                        s = retrieveAtomExpr(atom);
                    }
                    tuple = product(tuple, s);
                    // reject tuple if sig is missing
                    if (!hasAtom(lower, e.t.atom(i))) {
                        sigMissingAndTupleInvalid = true;
                        // stop handling this tuple (inner loop)
                        break;
                    }
                }
                if (!sigMissingAndTupleInvalid && tuple != null) {
                    Expr bound = tuple.in(e.f);
                    lowerBound = and(lowerBound, bound);
                }
            }
        }

        Decl decl = new Decl(null, new Pos("disjoint", 0, 0), null, null, new ArrayList<>(vars.values()), Sig.UNIV.oneOf());
        lowerBound = ExprQt.Op.SOME.make(null, null, Arrays.asList(decl), lowerBound);
        return lowerBound;
    }

    /**
     * retrieves an expression for the given atom
     *
     * this can be a signature or a constant, e.g., integer
     *
     * @param atom
     * @return
     */
    private Expr retrieveAtomExpr(String atom) {
        Expr e = loneSig.get(atom);
        if (e == null) {
            try {
                int i = Integer.parseInt(atom);
                e = ExprConstant.makeNUMBER(i);
            } catch (Exception e2) {
            }
        }
        if (e == null && atom.endsWith("$0")) {
            String baseName = atom.replaceAll("\\$0", "");
            for (String name : enumVal.keySet()) {
                if (name.endsWith("/" + baseName)) {
                    e = enumVal.get(name);
                }
            }
        }
        return e;
    }

    /**
     * check if atom is built in or can be instantiated with given sigs in bound
     *
     * @param bound
     * @param atom
     * @return
     */
    private boolean hasAtom(List<BoundElement> bound, String atom) {
        // if it is not a signature it is always present
        if (!(retrieveAtomExpr(atom) instanceof PrimSig)) {
            return true;
        }
        for (BoundElement be : bound) {
            if (atom.equals(be.atomName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * check if atom is built in or can be instantiated with given sigs
     *
     * @param cmdSigs
     * @param atom
     * @return
     */
    private boolean hasSigForAtom(List<Sig> cmdSigs, String atom) {
        Expr atomExpr = retrieveAtomExpr(atom);
        // if it is not a signature it is always present
        if (!(atomExpr instanceof PrimSig)) {
            return true;
        }
        // enum vals are also always present
        if (isEnumConstant(atomExpr)) {
            return true;
        }
        for (Sig sig : cmdSigs) {
            if (atom.equals(sig.label)) {
                return true;
            }
        }
        return false;
    }

    public List<BoundElement> getLowerBound() {
        return this.lower;
    }

    public List<BoundElement> getUpperBound() {
        if (UBKind.EXACT.equals(ubKind)) {
            List<BoundElement> tmp = new ArrayList<>(upperOrig);
            tmp.removeAll(upper);
            return tmp;
        }
        return this.upper;
    }

    public String printUpperBound() {
        if (UBKind.EXACT.equals(ubKind)) {
            Map<Object,String> restriction = new LinkedHashMap<>();
            for (BoundElement be : upper) {
                Object key = be.s;
                if (key == null) {
                    key = be.f;
                }
                String res = restriction.get(key);
                if (res == null) {
                    res = key + " ∌ ";
                }
                res += be + ", ";
                restriction.put(key, res);
            }
            return restriction.values().toString();
        }
        return getUpperBound().toString();
    }

    public ConstList<Sig> getSigsOrig() {
        return sigsOrig;
    }

    public Command getCmdOrig() {
        return cmdOrig;
    }

    public A4Solution getInstOrig() {
        return instOrig;
    }

    public HashMap<A4Tuple,String> getLowerBoundOriginMap() {
        HashMap<A4Tuple,String> map = new HashMap<A4Tuple,String>();
        for (BoundElement lb : getLowerBound()) {
            if (lb.s != null)
                map.put(lb.t, lb.s.toString());
            else
                map.put(lb.t, lb.f.toString());
        }
        return map;
    }

    public HashMap<A4Tuple,Sig> getLowerBoundSigs() {
        HashMap<A4Tuple,Sig> map = new HashMap<A4Tuple,Sig>();
        for (BoundElement lb : getLowerBound()) {
            if (lb.s != null)
                map.put(lb.t, lb.s);
        }
        return map;
    }

    public HashMap<A4Tuple,Field> getLowerBoundFields() {
        HashMap<A4Tuple,Field> map = new HashMap<A4Tuple,Field>();
        for (BoundElement lb : getLowerBound()) {
            if (lb.f != null)
                map.put(lb.t, lb.f);
        }
        return map;
    }

    public ArrayList<String> getUpperBoundNames() {
        ArrayList<String> names = new ArrayList<String>();
        for (BoundElement ub : getUpperBound()) {
            names.add(ub.toString());
        }
        return names;
    }

    /**
     * obtain a different abstract instance, if one exists
     *
     * requires prior minimization
     *
     * @return true if a new abstract instance was found, false if no further
     *         abstract instances exist
     */
    public boolean next() {
        List<Sig> sigsWithLonesForBounds = new ArrayList<>(sigsOrig);
        Command addedBoundsCmd = addBounds(cmdOrig, lower, upper, sigsWithLonesForBounds);
        Command newInstCmd = addedBoundsCmd.change(cmdOrig.formula.and(addedBoundsCmd.formula.not()));
        A4Solution ans = TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, sigsWithLonesForBounds, newInstCmd, optOrig);
        if (ans.satisfiable()) {
            this.sigsOrig = ConstList.make(sigsWithLonesForBounds);
            // TODO this might not be necessary and even better for performance if we don't add the negation, however in that case we need to keep the constraints elsewhere for the next call of next() where we need to use them
            this.cmdOrig = newInstCmd;
            this.instOrig = ans;

            // init bounds without creating lone sigs
            this.instance = new ArrayList<>();
            this.lower = new ArrayList<>();
            this.upper = new ArrayList<>();
            initBounds(ans);
            // do actual minimization cycle
            minLowerUpper(ubKind);

            return true;
        }
        return false;
    }

    public UBKind getUbKind() {
        return ubKind;
    }
}
