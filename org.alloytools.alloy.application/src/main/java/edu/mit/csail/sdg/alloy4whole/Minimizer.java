package edu.mit.csail.sdg.alloy4whole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.ast.Attr;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.Sig.PrimSig;
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
     * signatures of the original problem
     */
    private ConstList<Sig>             sigsOrig;
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

    protected Map<String,BoundElement> boundElem4Atom = new LinkedHashMap<>();
    protected Map<String,PrimSig>      oneSig         = new LinkedHashMap<>();
    protected Map<String,PrimSig>      loneSig        = new LinkedHashMap<>();

    protected class BoundElement {

        Field   f;
        Sig     s;
        A4Tuple t;

        boolean isAtom() {
            return t.arity() == 1;
        }

        String atomName() {
            return t.atom(0);
        }

        @Override
        public String toString() {
            if (t != null) {
                return t.toString();
            } else if (s != null) {
                return "UB for " + s.toString();
            } else {
                return "UB for " + f.toString();
            }
        }

    }

    A4Reporter rep = new A4Reporter() {

        // For example, here we choose to display each "warning" by printing
        // it to System.out
        @Override
        public void warning(ErrorWarning msg) {
            System.out.print("Relevance Warning:\n" + (msg.toString().trim()) + "\n\n");
            System.out.flush();
        }

        //        @Override
        //        public void bound(String msg) {
        //            System.out.println(msg);
        //        }

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
                cmdWithBounds = addBounds(cmdOrig.change(cmdOrig.formula.not()), candidate, upper, cmdSigs);
                cmdSanity = addBounds(cmdOrig, candidate, upper, new ArrayList<>());
            } else {
                cmdWithBounds = addBounds(cmdOrig.change(cmdOrig.formula.not()), lower, candidate, cmdSigs);
                cmdSanity = addBounds(cmdOrig, lower, candidate, new ArrayList<>());
            }

            A4Solution ansSanity = TranslateAlloyToKodkod.execute_command(rep, cmdSigs, cmdSanity, optOrig);
            if (!ansSanity.satisfiable()) {
                throw new RuntimeException("Unexpected UNSAT result of problem with new bounds that should include the original instance.");
            }

            A4Solution ans = TranslateAlloyToKodkod.execute_command(rep, cmdSigs, cmdWithBounds, optOrig);
            return !ans.satisfiable();
        }
    }

    public static void main(String[] args) {
        Module world = CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, "../org.alloytools.alloy.extra/extra/models/book/chapter2/addressBook3c.als");
        Command command = world.getAllCommands().get(world.getAllCommands().size() - 1);
        A4Options options = new A4Options();
        options.solver = A4Options.SatSolver.SAT4J;

        Minimizer m = new Minimizer();
        m.minimize(world.getAllReachableSigs(), command, options);

        printBounds(m);
    }

    private static void printBounds(Minimizer m) {
        System.out.println("LB = " + m.lower);
        System.out.println("UB = " + m.upper);
    }

    public void minimize(ConstList<Sig> sigs, Command cmd, A4Options opt) {
        this.sigsOrig = sigs;
        this.cmdOrig = cmd;
        this.optOrig = opt;
        A4Solution ans = TranslateAlloyToKodkod.execute_command(rep, sigs, cmd, opt);

        initBounds(ans);

        boolean rerun = true;
        while (rerun) {
            rerun = false;
            DdminAbsInstBounds min = new DdminAbsInstBounds(true);
            List<BoundElement> newLower = min.minimize(lower);
            if (lower.size() > newLower.size()) {
                rerun = true;
                lower = newLower;
            }

            min = new DdminAbsInstBounds(false);
            List<BoundElement> newUpper = min.minimize(upper);
            if (upper.size() > newUpper.size()) {
                rerun = true;
                upper = newUpper;
            } else {
                // no change in upper; this means lower is still valid
                rerun = false;
            }
            printBounds(this);
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
                        BoundElement e = new BoundElement();
                        e.s = s;
                        e.t = t;
                        // we only do it for the actual signature otherwise
                        // this would lead to creating a tuple twice or multiple times if it shows
                        // up multiple times across the inheritance hierarchy
                        if (instanceOf(e, s)) {
                            lower.add(e);
                            boundElem4Atom.put(e.atomName(), e);
                            oneSig.put(e.atomName(), new Sig.PrimSig(e.atomName(), (PrimSig) e.s, Attr.ONE));
                            loneSig.put(e.atomName(), new Sig.PrimSig(e.atomName(), (PrimSig) e.s, Attr.LONE));
                        }
                    }
                }
                for (Field f : s.getFields()) {
                    for (A4Tuple t : ans.eval(f)) {
                        if (t != null) {
                            BoundElement e = new BoundElement();
                            e.f = f;
                            e.t = t;
                            lower.add(e);
                        }
                    }
                }
            }
        }

        // copy lower bounds as instance
        instance.addAll(lower);

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
        return e.atomName().startsWith(s.label.replaceAll("this/", ""));
    }

    /**
     * checks relevance of signatures for computing bounds
     *
     * bounds should be ignored for signatures where this returns false, e.g.,
     * built-in or meta sigs
     *
     * @param s
     * @return
     */
    private boolean isRelevant(Sig s) {
        if (s.builtin) {
            return false;
        }
        if (s.isMeta != null) {
            return false;
        }
        // TODO this is just a guess
        if (s.toString().endsWith("/Ord")) {
            return false;
        }

        return true;
    }

    public Command addBounds(Command cmd, List<BoundElement> lower, List<BoundElement> upper, List<Sig> cmdSigs) {
        Expr lowerBound = null;
        Expr upperBound = null;

        // lower bounds require things to be there
        for (BoundElement e : lower) {
            if (e.isAtom()) {
                // atoms only added as singleton sigs
                cmdSigs.add(oneSig.get(e.atomName()));
            } else {
                // fields added as constraints
                Expr tuple = null;
                boolean sigMissingAndTupleInvalid = false;
                for (int i = 0; i < e.t.arity(); i++) {
                    PrimSig s = oneSig.get(e.t.atom(i));
                    if (tuple == null) {
                        tuple = s;
                    } else {
                        tuple = tuple.product(s);
                    }
                    // reject tuple if sig is missing
                    if (!hasAtom(lower, e.t.atom(i))) {
                        sigMissingAndTupleInvalid = true;
                        // stop handling this tuple (inner loop)
                        break;
                    }
                }
                if (!sigMissingAndTupleInvalid) {
                    Expr bound = tuple.in(e.f);
                    if (lowerBound == null) {
                        lowerBound = bound;
                    } else {
                        lowerBound = lowerBound.and(bound);
                    }
                }
            }
        }

        // upper bounds put a restriction on max from the instance
        for (BoundElement e : upper) {
            if (e.s != null) {
                // we need to bound the signature to the upper bound of the instance
                Expr atMost = null;
                for (BoundElement ie : instance) {
                    // only care about this signature
                    if (ie.s == e.s && instanceOf(ie, e.s)) {
                        PrimSig s = oneSig.get(ie.atomName());
                        if (!cmdSigs.contains(s)) {
                            // switch to the lone version for upper bound
                            s = loneSig.get(ie.atomName());
                            cmdSigs.add(s);
                        }
                        if (atMost == null) {
                            atMost = s;
                        } else {
                            atMost = atMost.plus(s);
                        }
                    }
                }
                // collect union of subsignatures of s
                Expr subs = null;
                for (Sig ch : sigsOrig) {
                    if (ch instanceof PrimSig) {
                        PrimSig pch = (PrimSig) ch;
                        if (pch.parent == e.s) {
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
                    bound = e.s.no();
                } else if (atMost == null) {
                    // there is no atom in e.s, but maybe in its subsigs
                    // we set s = children of s
                    bound = e.s.equal(subs);
                } else {
                    // we set s in (atoms + children sigs)
                    bound = e.s.in(atMost.plus(subs));
                }
                if (upperBound == null) {
                    upperBound = bound;
                } else {
                    upperBound = upperBound.and(bound);
                }
            }
        }

        Expr f = cmd.formula;
        if (lowerBound != null) {
            f = f.and(lowerBound);
            System.out.println("LB for check: one sigs for atoms of " + atomsOf(lower) + " and " + lowerBound);
        } else {
            System.out.println("LB for check: one sigs for atoms of " + atomsOf(lower));
        }
        System.out.println("UB for check: " + upperBound);
        if (upperBound != null) {
            f = f.and(upperBound);
        }
        return cmd.change(f);
    }

    private List<BoundElement> atomsOf(List<BoundElement> l) {
        return l.stream().filter(p -> p.isAtom()).collect(Collectors.toList());
    }

    private boolean hasAtom(List<BoundElement> bound, String atom) {
        for (BoundElement be : bound) {
            if (atom.equals(be.atomName())) {
                return true;
            }
        }
        return false;
    }

    public List<BoundElement> getLowerBound() {
        return this.lower;
    }

    public List<BoundElement> getUpperBound() {
        return this.upper;
    }
}


