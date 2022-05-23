package org.alloytools.alloy.absinst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Attr;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Decl;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprConstant;
import edu.mit.csail.sdg.ast.ExprList;
import edu.mit.csail.sdg.ast.ExprQt;
import edu.mit.csail.sdg.ast.ExprVar;
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
     * the kind of upper bound we use (different interpretation of bound elements in
     * upper)
     */
    private UBKind                     ubKind         = UBKind.EXACT;

    protected Map<String,BoundElement> boundElem4Atom = new LinkedHashMap<>();
    protected Map<String,PrimSig>      oneSig         = new LinkedHashMap<>();
    protected Map<String,PrimSig>      loneSig        = new LinkedHashMap<>();

    private A4Solution                 instOrig;

    protected class BoundElement {

        Field   f;
        Sig     s;
        A4Tuple t;
        String  atomName;

        boolean isAtom() {
            if (atomName != null) {
                return true;
            }
            return t.arity() == 1;
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
            } else if (s != null) {
                return "UB for " + s.toString();
            } else {
                return "UB for " + f.toString();
            }
        }

    }

    private String boundMsg;
    A4Reporter     rep = new A4Reporter() {


                           // For example, here we choose to display each "warning" by printing
                           // it to System.out
                           @Override
                           public void warning(ErrorWarning msg) {
                               System.out.print("Relevance Warning:\n" + (msg.toString().trim()) + "\n\n");
                               System.out.flush();
                           }

                           @Override
                           public void bound(String msg) {
                               if (boundMsg.isBlank()) {
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
                cmdSanity = addBounds(cmdOrig, candidate, upper, new ArrayList<>());
            } else {
                cmdWithBounds = addBounds(negatePred(cmdOrig), lower, candidate, cmdSigs);
                cmdSanity = addBounds(cmdOrig, lower, candidate, new ArrayList<>());
            }

            A4Solution ansSanity = TranslateAlloyToKodkod.execute_command(rep, cmdSigs, cmdSanity, optOrig);
            if (!ansSanity.satisfiable()) {
                throw new RuntimeException("Unexpected UNSAT result of problem with new bounds that should include the original instance.");
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
        m.minimize(world.getAllReachableSigs(), world.getAllReachableFacts(), command, options);

        printBounds(m);
    }

    private static void printBounds(Minimizer m) {
        System.out.println("LB = " + m.lower);
        System.out.println("UB = " + m.upper);
    }

    /**
     * minimization of bounds (uses legacy UBKind = INSTANCE_OR_NO_UPPER)
     *
     * @param sigs
     * @param facts
     * @param cmd
     * @param opt
     */
    public void minimize(ConstList<Sig> sigs, Expr facts, Command cmd, A4Options opt) {
        minimize(sigs, facts, cmd, opt, UBKind.INSTANCE_OR_NO_UPPER);
    }

    public void minimize(ConstList<Sig> sigs, Expr facts, Command cmd, A4Options opt, UBKind ub) {
        this.sigsOrig = sigs;
        this.factsOrig = facts;
        this.cmdOrig = cmd;
        this.optOrig = opt;
        this.boundMsg = "";
        this.ubKind = ub;
        A4Solution ans = TranslateAlloyToKodkod.execute_command(rep, sigs, cmd, opt);
        instOrig = ans;

        initBounds(ans);

        {
            // sanity check that within given bounds we have some valid instance
            ArrayList<Sig> sigsSanity = new ArrayList<>(sigs);
            Command cmdSanity = addBounds(cmd, lower, upper, sigsSanity);
            A4Solution ansSanity = TranslateAlloyToKodkod.execute_command(rep, sigsSanity, cmdSanity, opt);
            if (!ansSanity.satisfiable()) {
                throw new RuntimeException("Problem unsat with original bounds.");
            }
        }
        {
            // sanity check that within given bounds we DON'T have any invalid instance
            // TODO remove, but keep it where UB is of kind NO_UPPER
            DdminAbsInstBounds ddmin = new DdminAbsInstBounds(false);
            boolean check = ddmin.check(upper); // all instances in bounds satisfy command
            if (!check) {
                throw new RuntimeException("Instances in initial bounds that violate the command (maybe UB kind is NO_UPPER?).");
            }
        }


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
                if (isRelevant(s)) {
                    for (String atom : getAtoms(s, boundMsg)) {
                        if (instanceOf(atom, s)) {
                            BoundElement es = new BoundElement();
                            es.s = s;
                            es.atomName = atom;
                            upper.add(es);
                            // add as lone sig in case it is neede later
                            if (loneSig.get(es.atomName()) == null) {
                                loneSig.put(es.atomName(), new Sig.PrimSig(es.atomName(), (PrimSig) es.s, Attr.LONE));
                            }
                            System.out.println(atom);
                        }
                    }
                    // TODO build the whole UB also including tuples
                    for (Field f : s.getFields()) {
                        BoundElement ef = new BoundElement();
                        ef.f = f;
                        upper.add(ef);
                    }
                }
            }
        }
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
            String ini = "Sig " + s.label + " in ";
            if (line.startsWith(ini)) {
                line = line.substring(ini.length());
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
                cmdSigs.add(oneSig.get(e.atomName()));
            } else {
                // fields added as constraints
                Expr tuple = null;
                boolean sigMissingAndTupleInvalid = false;
                for (int i = 0; i < e.t.arity(); i++) {
                    Expr s = retrieveAtomExpr(e.t.atom(i), true);
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
        if (UBKind.INSTANCE.equals(ubKind) || UBKind.INSTANCE_OR_NO_UPPER.equals(ubKind)) {
            for (BoundElement e : upper) {
                if (e.s != null) {
                    upperBound = addSigUBFromInstance(cmdSigs, upperBound, e);
                } else {
                    upperBound = addFieldUBFromInstance(cmdSigs, upperBound, e);
                }
            }
        } else if (UBKind.EXACT.equals(ubKind)) {
            Map<Sig,Expr> atomsForSig = new LinkedHashMap<>();
            for (BoundElement e : upper) {
                if (e.isAtom()) {
                    Expr atMost = atomsForSig.get(e.s);
                    PrimSig s = oneSig.get(e.atomName());
                    if (!cmdSigs.contains(s)) {
                        // switch to the lone version for upper bound
                        s = loneSig.get(e.atomName());
                        cmdSigs.add(s);
                    }
                    if (atMost == null) {
                        atMost = s;
                    } else {
                        atMost = atMost.plus(s);
                    }
                    atomsForSig.put(e.s, atMost);
                }
            }
            for (Sig s : atomsForSig.keySet()) {
                upperBound = boundSigToAtMostExpr(upperBound, s, atomsForSig.get(s));
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

    private Expr addFieldUBFromInstance(List<Sig> cmdSigs, Expr upperBound, BoundElement e) {
        Expr atMost = null;
        // find instance elements for field (from upper bound)
        for (BoundElement ei : instance) {
            if (ei.f == e.f) {
                Expr tuple = null;
                for (int i = 0; i < ei.t.arity(); i++) {
                    // TODO construct tuple based on atoms
                    String atomName = ei.t.atom(i);
                    // check if one atom is used
                    Expr atom = oneSig.get(atomName);
                    // atom not found in sigs or one sig not included
                    if (atom == null || !cmdSigs.contains(atom)) {
                        atom = retrieveAtomExpr(atomName, false);
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
        upperBound = boundSigToAtMostExpr(upperBound, e.s, atMost);
        return upperBound;
    }

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
        if (upperBound == null) {
            upperBound = bound;
        } else {
            upperBound = upperBound.and(bound);
        }
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
        System.out.println("LB for check: " + lowerBound);
        if (upperBound != null) {
            f = f.and(upperBound);
        }
        System.out.println("UB for check: " + upperBound);
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
                        s = retrieveAtomExpr(atom, true);
                    }
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
     * @param useOneSig in case of signatures use one (lone if false)
     * @return
     */
    private Expr retrieveAtomExpr(String atom, boolean useOneSig) {
        Expr e = null;
        if (useOneSig) {
            e = oneSig.get(atom);
        } else {
            e = loneSig.get(atom);
        }
        if (e == null) {
            try {
                int i = Integer.parseInt(atom);
                e = ExprConstant.makeNUMBER(i);
            } catch (Exception e2) {
            }
        }
        return e;
    }

    private List<BoundElement> atomsOf(List<BoundElement> l) {
        return l.stream().filter(p -> p.isAtom()).collect(Collectors.toList());
    }

    private boolean hasAtom(List<BoundElement> bound, String atom) {
        // if it is not a signature it is always present
        if (!(retrieveAtomExpr(atom, true) instanceof PrimSig)) {
            return true;
        }
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
}
