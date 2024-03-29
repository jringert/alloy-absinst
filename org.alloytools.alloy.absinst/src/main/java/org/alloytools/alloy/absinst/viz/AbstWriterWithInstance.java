/* Alloy Analyzer 4 -- Copyright (c) 2006-2009, Felix Chang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.alloytools.alloy.absinst.viz;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorAPI;
import edu.mit.csail.sdg.alloy4.ErrorFatal;
import edu.mit.csail.sdg.alloy4.SafeList;
import edu.mit.csail.sdg.alloy4.Util;
import edu.mit.csail.sdg.alloy4.Version;
import edu.mit.csail.sdg.ast.Expr;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Func;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Sig.Field;
import edu.mit.csail.sdg.ast.Sig.PrimSig;
import edu.mit.csail.sdg.ast.Sig.SubsetSig;
import edu.mit.csail.sdg.ast.Type;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4Tuple;
import edu.mit.csail.sdg.translator.A4TupleSet;

/**
 * This helper class contains helper routines for writing an A4Solution object
 * out as an XML file.
 *
 * @modified [electrum] prints full trace instance into XML; each state is added
 *           as an XML Instance element; trace meta-data (length, loop, variable
 *           elements) is also printed;
 *
 *           writing of skolems has been tweaked, in two different scenarios:
 *           static skolem vars (from existential quantifications) may have
 *           atoms assigned that do not exist in every state (so it is no longer
 *           enforced that skolems always belong to a sig); mutable skolem vars
 *           (from auxiliary functions) may be empty in certain states and
 *           should still be printed (so empty skolem vars are always printed);
 */

public final class AbstWriterWithInstance {

    /** Maps each Sig, Field, and Skolem to a unique id. */
    private final IdentityHashMap<Expr,String> map       = new IdentityHashMap<Expr,String>();

    /** This is the solution we're writing out. */
    private final A4Solution                   sol;

    /**
     * This is the A4Reporter that we're sending diagnostic messages to; can be null
     * if none.
     */
    private final A4Reporter                   rep;

    /** This is the list of toplevel sigs. */
    private final List<PrimSig>                toplevels = new ArrayList<PrimSig>();

    /** This is the output file. */
    private final PrintWriter                  out;

    /**
     * Helper method that returns a unique id for the given Sig, Field, or Skolem.
     */
    private String map(Expr obj) {
        String id = map.get(obj);
        if (id == null) {
            id = Integer.toString(map.size());
            map.put(obj, id);
        }
        return id;
    }

    /**
     * Helper method that returns the list of direct subsignatures.
     */
    private Iterable<PrimSig> children(PrimSig x) throws Err {
        if (x == Sig.NONE)
            return new ArrayList<PrimSig>();
        if (x != Sig.UNIV)
            return x.children();
        else
            return toplevels;
    }

    /** Write the given Expr and its Type. */
    private boolean writeExpr(String prefix, Expr expr, int state) throws Err {
        Type type = expr.type();
        if (!type.hasTuple())
            return false;
        if (sol != null) {
            // Check to see if the tupleset is *really* fully contained inside
            // "type".
            // If not, then grow "type" until the tupleset is fully contained
            // inside "type"
            Expr sum = type.toExpr();
            int lastSize = (-1);
            while (true) {
                A4TupleSet ts = (A4TupleSet) (sol.eval(expr.minus(sum), state));
                int n = ts.size();
                if (n <= 0 || expr instanceof ExprVar) // [electrum] static skolem vars (from quantifications) may not be part of the sig in other states
                    break;
                if (lastSize > 0 && lastSize <= n)
                    throw new ErrorFatal("An internal error occurred in the evaluator.");
                lastSize = n;
                Type extra = ts.iterator().next().type();
                type = type.merge(extra);
                sum = sum.plus(extra.toExpr());
            }
            // Now, write out the tupleset
            A4TupleSet ts = (A4TupleSet) (sol.eval(expr, state));
            // [electrum] force printing of element even if ts empty, otherwise mutable skolem funs missing from certain steps
            if (prefix.length() > 0) {
                out.print(prefix);
                prefix = "";
            }
            for (A4Tuple t : ts) {
                out.print("   <tuple>");
                for (int i = 0; i < t.arity(); i++)
                    Util.encodeXMLs(out, " <atom label=\"", t.atom(i), "\"/>");
                out.print(" </tuple>\n");
            }
        }
        // Now, write out the type
        if (prefix.length() > 0)
            return false;
        for (List<PrimSig> ps : type.fold()) {
            out.print("   <types>");
            for (PrimSig sig : ps)
                Util.encodeXMLs(out, " <type ID=\"", map(sig), "\"/>");
            out.print(" </types>\n");
        }
        return true;
    }

    private boolean writeExpr(String prefix, Expr expr, int state, HashMap<A4Tuple,String> lower) throws Err {
        Type type = expr.type();
        if (!type.hasTuple())
            return false;
        if (sol != null) {
            // Check to see if the tupleset is *really* fully contained inside
            // "type".
            // If not, then grow "type" until the tupleset is fully contained
            // inside "type"
            Expr sum = type.toExpr();
            int lastSize = (-1);
            while (true) {
                A4TupleSet ts = (A4TupleSet) (sol.eval(expr.minus(sum), state));
                int n = ts.size();
                if (n <= 0 || expr instanceof ExprVar) // [electrum] static skolem vars (from quantifications) may not be part of the sig in other states
                    break;
                if (lastSize > 0 && lastSize <= n)
                    throw new ErrorFatal("An internal error occurred in the evaluator.");
                lastSize = n;
                Type extra = ts.iterator().next().type();
                type = type.merge(extra);
                sum = sum.plus(extra.toExpr());
            }
            // Now, write out the tupleset
            A4TupleSet ts = (A4TupleSet) (sol.eval(expr, state));
            // [electrum] force printing of element even if ts empty, otherwise mutable skolem funs missing from certain steps
            if (prefix.length() > 0) {
                out.print(prefix);
                prefix = "";
            }
            for (A4Tuple t : ts) {
                //To avoid duplicate relations in visualization, do not print relation if in lower bound.
                boolean inLower = false;
                for (A4Tuple lb : lower.keySet()) {
                    //if (t.toString().contentEquals(lb.toString())) {
                    if (t.toString().contentEquals(lower.get(lb).toString())) {
                        inLower = true;
                        break;
                    }
                }

                if (!inLower) {
                    out.print("   <tuple>");
                    for (int i = 0; i < t.arity(); i++)
                        Util.encodeXMLs(out, " <atom label=\"", t.atom(i), "\"/>");
                    out.print(" </tuple>\n");
                }
            }
        }
        // Now, write out the type
        if (prefix.length() > 0)
            return false;
        for (List<PrimSig> ps : type.fold()) {
            out.print("   <types>");
            for (PrimSig sig : ps)
                Util.encodeXMLs(out, " <type ID=\"", map(sig), "\"/>");
            out.print(" </types>\n");
        }
        return true;
    }
    /** Write the given Sig. */
    private A4TupleSet writeSig(final Sig x, int state, HashMap<A4Tuple,String> lower) throws Err {
        A4TupleSet ts = null, ts2 = null;
        if (x == Sig.NONE)
            return null; // should not happen, but we test for it anyway
        if (sol == null && x.isMeta != null)
            return null; // When writing the metamodel, skip the metamodel sigs!
        if (x instanceof PrimSig)
            for (final PrimSig sub : children((PrimSig) x)) {
                A4TupleSet ts3 = writeSig(sub, state, lower);
                if (ts2 == null)
                    ts2 = ts3;
                else
                    ts2 = ts2.plus(ts3);
            }
        if (rep != null)
            rep.write(x);


        Util.encodeXMLs(out, "\n<sig label=\"", x.label, "\" ID=\"", map(x));
        if (x instanceof PrimSig && x != Sig.UNIV)
            Util.encodeXMLs(out, "\" parentID=\"", map(((PrimSig) x).parent));
        if (x.builtin)
            out.print("\" builtin=\"yes");
        if (x.isAbstract != null)
            out.print("\" abstract=\"yes");
        if (x.isOne != null)
            out.print("\" one=\"yes");
        if (x.isLone != null)
            out.print("\" lone=\"yes");
        if (x.isSome != null)
            out.print("\" some=\"yes");
        if (x.isPrivate != null)
            out.print("\" private=\"yes");
        if (x.isMeta != null)
            out.print("\" meta=\"yes");
        if (x instanceof SubsetSig && ((SubsetSig) x).exact)
            out.print("\" exact=\"yes");
        if (x.isEnum != null)
            out.print("\" enum=\"yes");
        if (x.isVariable != null)
            out.print("\" var=\"yes");
        out.print("\">\n");
        try {
            if (sol != null && x != Sig.UNIV && x != Sig.SIGINT && x != Sig.SEQIDX) {
                ts = (sol.eval(x, state));
                for (A4Tuple t : ts.minus(ts2)) {
                    boolean inLower = false;
                    for (A4Tuple lb : lower.keySet()) {
                        if (lb.toString().equals(t.toString())) {
                            inLower = true;
                            break;
                        }
                    }
                    //if (!inLower)
                    Util.encodeXMLs(out, "   <atom label=\"", t.atom(0), "\"/>\n");
                }

            }
        } catch (Throwable ex) {
            throw new ErrorFatal("Error evaluating sig " + x.label, ex);
        }
        if (x instanceof SubsetSig)
            for (Sig p : ((SubsetSig) x).parents)
                Util.encodeXMLs(out, "   <type ID=\"", map(p), "\"/>\n");
        out.print("</sig>\n");

        for (Field field : x.getFields()) {
            writeField(field, state, lower);
        }

        return ts;
    }

    /** Write the given Field. */
    private void writeField(Field x, int state, HashMap<A4Tuple,String> lower) throws Err {
        try {
            if (sol == null && x.isMeta != null)
                return; // when writing the metamodel, skip the metamodel
                       // fields!
            if (x.type().hasNoTuple())
                return; // we do not allow "none" in the XML file's type
                       // declarations
            if (rep != null)
                rep.write(x);
            Util.encodeXMLs(out, "\n<field label=\"", x.label, "\" ID=\"", map(x), "\" parentID=\"", map(x.sig));
            if (x.isPrivate != null)
                out.print("\" private=\"yes");
            if (x.isMeta != null)
                out.print("\" meta=\"yes");
            if (x.isVariable != null)
                out.print("\" var=\"yes");
            out.print("\">\n");
            writeExpr("", x, state, lower);
            out.print("</field>\n");
        } catch (Throwable ex) {
            throw new ErrorFatal("Error evaluating field " + x.sig.label + "." + x.label, ex);
        }
    }

    /** Write the given Skolem. */
    private void writeSkolem(ExprVar x, int state) throws Err {
        try {
            if (sol == null)
                return; // when writing a metamodel, skip the skolems
            if (x.type().hasNoTuple())
                return; // we do not allow "none" in the XML file's type
                       // declarations
            StringBuilder sb = new StringBuilder();
            Util.encodeXMLs(sb, "\n<skolem label=\"", x.label, "\" ID=\"", map(x), "\">\n");
            if (writeExpr(sb.toString(), x, state)) {
                out.print("</skolem>\n");
            }
        } catch (Throwable ex) {
            throw new ErrorFatal("Error evaluating skolem " + x.label, ex);
        }
    }

    /**
     * If sol==null, write the list of Sigs as a Metamodel, else write the solution
     * as an XML file.
     */
    private AbstWriterWithInstance(A4Reporter rep, A4Solution sol, Iterable<Sig> sigs, int bitwidth, int maxseq, int mintrace, int maxtrace, int tracelength, int backloop, String originalCommand, String originalFileName, PrintWriter out, Iterable<Func> extraSkolems, int state, HashMap<A4Tuple,String> lower, ArrayList<String> upper) throws Err {
        this.rep = rep;
        this.out = out;
        this.sol = sol;
        for (Sig s : sigs)
            if (s instanceof PrimSig && ((PrimSig) s).parent == Sig.UNIV)
                toplevels.add((PrimSig) s);
        // [electrum] write temporal metadata
        out.print("<instance bitwidth=\"");
        out.print(bitwidth);
        out.print("\" maxseq=\"");
        out.print(maxseq);
        out.print("\" mintrace=\"");
        out.print(mintrace);
        out.print("\" maxtrace=\"");
        out.print(maxtrace);
        out.print("\" command=\"");
        Util.encodeXML(out, originalCommand);
        out.print("\" filename=\"");
        Util.encodeXML(out, originalFileName);
        out.print("\" tracelength=\"");
        out.print(tracelength);
        out.print("\" backloop=\"");
        out.print(backloop);
        if (sol == null)
            out.print("\" metamodel=\"yes");
        out.print("\">\n");
        writeSig(Sig.UNIV, state, lower);
        for (Sig s : sigs)
            if (s instanceof SubsetSig)
                writeSig(s, state, lower);
        int m = 0;

        //Add lower bound skolems
        for (A4Tuple lb : lower.keySet()) {
            StringBuilder sb = new StringBuilder();
            String label = "$LB " + lower.get(lb).toString();
            out.print("\n<skolem label=\"" + label + "\" ID=\"m" + m + "\">\n");
            out.print("   <tuple>");
            if (lb.arity() == 1) {
                Util.encodeXMLs(out, " <atom label=\"", lower.get(lb), "\"/>");
            } else {
                String[] a_labels = lower.get(lb).split("->");
                for (String l : a_labels)
                    Util.encodeXMLs(out, " <atom label=\"", l, "\"/>");

            }

            out.print(" </tuple>\n");
            for (List<PrimSig> ps : lb.type().fold()) {
                out.print("   <types>");
                for (PrimSig sig : ps) {
                    String id = "";
                    for (Expr e : map.keySet()) {
                        if (e.type().toString().equals(sig.type().toString())) {
                            id = map.get(e);
                            break;
                        }
                    }
                    Util.encodeXMLs(out, " <type ID=\"", id, "\"/>");
                }

                out.print(" </types>\n");
            }
            out.print("</skolem>\n");
            m++;
        }

        if (sol != null)
            for (ExprVar s : sol.getAllSkolems()) {
                if (rep != null)
                    rep.write(s);
                writeSkolem(s, state);
            }

        if (sol != null && extraSkolems != null)
            for (Func f : extraSkolems)
                if (f.count() == 0 && f.call().type().hasTuple()) {
                    String label = f.label;
                    while (label.length() > 0 && label.charAt(0) == '$')
                        label = label.substring(1);
                    label = "$" + label;
                    try {
                        if (rep != null)
                            rep.write(f.call());
                        StringBuilder sb = new StringBuilder();
                        Util.encodeXMLs(sb, "\n<skolem label=\"", label, "\" ID=\"m" + m + "\">\n");
                        if (writeExpr(sb.toString(), f.call(), state)) {
                            out.print("</skolem>\n");
                        }
                        m++;
                    } catch (Throwable ex) {
                        throw new ErrorFatal("Error evaluating skolem " + label, ex);
                    }
                }

        out.print("\n</instance>\n");
    }

    /**
     * If this solution is a satisfiable solution, this method will write it out in
     * XML format as a sequence of &lt;instance&gt;..&lt;/instance&gt;.
     */
    public static void writeInstance(A4Reporter rep, A4Solution sol, PrintWriter out, Iterable<Func> extraSkolems, Map<String,String> sources, HashMap<A4Tuple,String> lower, ArrayList<String> upper) throws Err {
        if (!sol.satisfiable())
            throw new ErrorAPI("This solution is unsatisfiable.");
        try {
            Util.encodeXMLs(out, "<alloy builddate=\"", Version.buildDate(), "\">\n\n");

            // [electrum] write all instances of the trace
            for (int i = 0; i < sol.getTraceLength(); i++)
                new AbstWriterWithInstance(rep, sol, sol.getAllReachableSigs(), sol.getBitwidth(), sol.getMaxSeq(), sol.getMinTrace(), sol.getMaxTrace(), sol.getTraceLength(), sol.getLoopState(), sol.getOriginalCommand(), sol.getOriginalFilename(), out, extraSkolems, i, lower, upper);
            if (sources != null)
                for (Map.Entry<String,String> e : sources.entrySet()) {
                    Util.encodeXMLs(out, "\n<source filename=\"", e.getKey(), "\" content=\"", e.getValue(), "\"/>\n");
                }
            out.print("\n</alloy>\n");
        } catch (Throwable ex) {
            if (ex instanceof Err)
                throw (Err) ex;
            else
                throw new ErrorFatal("Error writing the solution XML file.", ex);
        }
        if (out.checkError())
            throw new ErrorFatal("Error writing the solution XML file.");
    }

    public static void writeTheme(A4Solution sol, PrintWriter out, HashMap<A4Tuple,String> lowerSig, HashMap<A4Tuple,String> lowerField) {
        if (!sol.satisfiable())
            throw new ErrorAPI("This solution is unsatisfiable. Cannot create a theme.");
        try {
            out.print("\n<?xml version=\"1.0\"?>\n");
            out.print("\n<alloy>\n");
            out.print("\n<view>\n");

            out.print("\n<defaultnode color=\"Gray\"/>\n");
            out.print("\n<defaultedge color=\"Gray\"/>\n");

            out.print("\n<node>\n");
            for (Sig sig : sol.getAllReachableSigs()) {
                if(sig != Sig.NONE) {
                    String prettyPrintLabel = sig.label;
                    if (prettyPrintLabel.contains("this/"))
                        prettyPrintLabel = prettyPrintLabel.substring(prettyPrintLabel.indexOf("this/") + 5);
                    out.print("   <type name=\"" + prettyPrintLabel + "\"/>\n");
                    SafeList<PrimSig> list;
                    if (sig != Sig.UNIV && sig != Sig.SIGINT) {
                        list = ((PrimSig) sig).children();
                        for (PrimSig sub : list) {
                            prettyPrintLabel = sub.label;
                            if (prettyPrintLabel.contains("this/"))
                                prettyPrintLabel = prettyPrintLabel.substring(prettyPrintLabel.indexOf("this/") + 5);
                            out.print("   <type name=\"" + prettyPrintLabel + "\"/>\n");
                        }
                    }
                }
            }
            out.print("\n</node>\n");

            for (A4Tuple lb : lowerSig.keySet()) {
                out.print("\n<node color =\"Yellow\" label=\"$LB\">\n");
                String prettyPrintLabel = lowerSig.get(lb);
                if (prettyPrintLabel.contains("this/"))
                    prettyPrintLabel = prettyPrintLabel.substring(prettyPrintLabel.indexOf("this/") + 5);
                prettyPrintLabel = prettyPrintLabel.substring(0, prettyPrintLabel.indexOf("$"));

                out.print("   <set name=\"$LB " + lowerSig.get(lb) + "\" type=\"" + prettyPrintLabel + "\"/>");
                out.print("\n</node>\n");
            }

            for (A4Tuple lb : lowerField.keySet()) {
                out.print("\n<edge color =\"Black\" label=\"$LB " + lowerField.get(lb).split(":")[0] + "\">\n");
                out.print("   <relation name=\"$LB " + lowerField.get(lb).split(":")[1] + "\"> ");

                String[] prettyPrint = (lb.type().toString().substring(1, lb.type().toString().length() - 1)).split("->");
                for(String print : prettyPrint) {
                    if (print.contains("this/"))
                        print = print.substring(print.indexOf("this/") + 5);
                    out.print("<type name=\"" + print + "\"/> ");
                }
                out.print("</relation>\n");
                out.print("\n</edge>\n");
            }

            out.print("\n</view>\n");

            out.print("\n</alloy>\n");
        } catch (Throwable ex) {
            if (ex instanceof Err)
                throw (Err) ex;
            else
                throw new ErrorFatal("Error writing the solution theme file.", ex);
        }
        if (out.checkError())
            throw new ErrorFatal("Error writing the solution theme file.");
    }
}