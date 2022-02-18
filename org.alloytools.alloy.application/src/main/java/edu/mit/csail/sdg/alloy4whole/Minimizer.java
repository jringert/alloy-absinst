package edu.mit.csail.sdg.alloy4whole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.ConstList;
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

    private ConstList<Sig>             sigs;
    private Command                    cmd;
    private List<BoundElement>         lower          = new ArrayList<>();
    private List<BoundElement>         instance       = new ArrayList<>();
    private List<BoundElement>         upper          = new ArrayList<>();
    private A4Options                  opt;

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
            List<Sig> cmdSigs = new ArrayList<>(sigs);
            Command cmdWithBounds = null;
            if (low) {
                cmdWithBounds = addBounds(cmd.change(cmd.formula.not()), candidate, upper, cmdSigs);
            } else {
                cmdWithBounds = addBounds(cmd.change(cmd.formula.not()), lower, candidate, cmdSigs);
            }

            A4Solution ans = TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, cmdSigs, cmdWithBounds, opt);
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

        System.out.println("LB = " + m.lower);
        System.out.println("UB = " + m.upper);
    }

    public void minimize(ConstList<Sig> sigs, Command cmd, A4Options opt) {
        this.sigs = sigs;
        this.cmd = cmd;
        this.opt = opt;
        A4Solution ans = TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, sigs, cmd, opt);

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
        }
    }

    /**
     *
     * nonexistance for upper bounds -- we need calculated scopes -- we need to name
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
                        lower.add(e);
                        boundElem4Atom.put(e.atomName(), e);
                        oneSig.put(e.atomName(), new Sig.PrimSig(e.atomName(), (PrimSig) e.s, Attr.ONE));
                        loneSig.put(e.atomName(), new Sig.PrimSig(e.atomName(), (PrimSig) e.s, Attr.LONE));
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
        Expr f = cmd.formula;

        // lower bounds require things to be there
        for (BoundElement e : lower) {
            if (e.isAtom()) {
                // atoms only added as singleton sigs
                cmdSigs.add(oneSig.get(e.atomName()));
            } else {
                // fields added as constraints
                Expr tuple = null;
                for (int i = 0; i < e.t.arity(); i++) {
                    PrimSig s = oneSig.get(e.t.atom(i));
                    if (tuple == null) {
                        tuple = s;
                    } else {
                        tuple = tuple.product(s);
                    }
                    // add sings if necessary
                    // TODO be aware that this means we have to add these when presenting bounds to the user
                    if (!cmdSigs.contains(s)) {
                        cmdSigs.add(s);
                    }
                }
                f = f.and(tuple.in(e.f));
            }
        }

        // upper bounds put a restriction on max from the instance
        for (BoundElement e : upper) {
            if (e.s != null) {
                // we need to bound the signature to the upper bound of the instance
                Expr atMost = null;
                for (BoundElement ie : instance) {
                    // only care about this signature
                    if (ie.s == e.s) {
                        PrimSig s = oneSig.get(ie.atomName());
                        if (cmdSigs.contains(s)) {
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
                // there might not have been an atom in the instance so upper bound is empty set
                if (atMost == null) {
                    f = f.and(e.s.no());
                } else {
                    f = f.and(e.s.in(atMost));
                }
            }
        }

        return cmd.change(f);
    }

}


