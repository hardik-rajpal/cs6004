
// import java.util.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.TreeSet;

import soot.Body;
import soot.Local;
import soot.SootField;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.AbstractInvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JDynamicInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
// import soot.jimple.internal.AbstractDefinitionStmt;
// // import soot.jimple.AnyNewExpr;
// import soot.jimple.internal.JAssignStmt;
// import soot.jimple.internal.JDynamicInvokeExpr;
// import soot.jimple.internal.JIdentityStmt;
// import soot.jimple.internal.JInstanceFieldRef;
// import soot.jimple.internal.JInterfaceInvokeExpr;
// import soot.jimple.internal.JNewExpr;
// import soot.jimple.internal.JSpecialInvokeExpr;
// import soot.jimple.internal.JStaticInvokeExpr;
// import soot.jimple.internal.JVirtualInvokeExpr;
// import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.Chain;

public class MyVeryOwnEscapeAnalysis {
    PTA pta;

    public TreeSet<String> doEscapeAnalysis(Body body, TreeMap<Unit, PTA.NodePointsToData> pointsToInfo, PTA _pta) {
        pta = _pta;
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
        // escaping variables per unit.
        TreeSet<String> escapingVariables = new TreeSet<>();
        Chain<SootField> fields = body.getMethod().getDeclaringClass().getFields();
        for (Unit u : pointsToInfo.keySet()) {
            TreeSet<String> escapingVars = processUnit(pointsToInfo, graph, u, fields);
            escapingVariables.addAll(escapingVars);
        }
        return escapingVariables;
    }

    private TreeSet<String> processUnit(TreeMap<Unit, PTA.NodePointsToData> pointsToInfo, ExceptionalUnitGraph graph,
            Unit u, Chain<SootField> fields) {
        TreeSet<String> escapingVariables = new TreeSet<>();
        PTA.PointsToGraph goingIn = pointsToInfo.get(u).in;
        PTA.PointsToGraph goingOut = pointsToInfo.get(u).out;
        // should be objects only.
        // use the heapmap only to perform reachability checks.

        TreeSet<String> baseEscapingObjects = getEscapingVariables(u, goingIn);
        // all parameter dummies and globals escape.
        baseEscapingObjects.addAll(pta.getDummyHeapObjects(goingIn));

        // add non-dummy objs pointed to by fields:
        for (SootField field : fields) {
            String fieldName = field.getName();
            if (goingIn.stackMap.containsKey(fieldName)) {
                baseEscapingObjects.addAll(goingIn.stackMap.get(fieldName));
            }
            if (goingOut.stackMap.containsKey(fieldName)) {
                baseEscapingObjects.addAll(goingIn.stackMap.get(fieldName));
            }
        }

        ArrayList<Unit> descendants = getDescendants(u, graph);
        descendants.add(u);
        PTA.PointsToGraph ptgAfter = PTA.mergeOutsOf(descendants, pointsToInfo);
        TreeSet<String> reachableVars = new TreeSet<>();
        // reachable right before escaping node.
        reachableVars.addAll(getReachableVars(goingIn.heapMap, baseEscapingObjects));
        // reachable any point after escaping node.
        reachableVars.addAll(getReachableVars(ptgAfter.heapMap, baseEscapingObjects));

        escapingVariables.addAll(reachableVars);

        // remove all dummy objects:
        escapingVariables.removeIf(s -> {
            return s.contains("@");
        });
        return escapingVariables;
    }

    private void dfs2(String rv, TreeSet<String> reachableVars, TreeMap<PTA.HeapReference, TreeSet<String>> heapMap) {
        for (PTA.HeapReference hr : heapMap.keySet()) {
            if (hr.object.equals(rv)) {
                TreeSet<String> newRV = heapMap.get(hr);
                for (String newrv : newRV) {
                    if (!reachableVars.contains(newrv)) {
                        reachableVars.add(newrv);
                        dfs2(newrv, reachableVars, heapMap);
                    }
                }
            }
        }
    }

    private TreeSet<String> getReachableVars(TreeMap<PTA.HeapReference, TreeSet<String>> heapMap,
            TreeSet<String> baseEscapingObjects) {
        // expects baseEscapingVariables to be on the heap.
        TreeSet<String> reachableVars = new TreeSet<>(baseEscapingObjects);

        for (String rv : baseEscapingObjects) {
            dfs2(rv, reachableVars, heapMap);
        }
        return reachableVars;
    }

    void dfs(Unit u, ExceptionalUnitGraph graph, TreeSet<Unit> descendants) {
        for (Unit desc : graph.getSuccsOf(u)) {
            if (!descendants.contains(desc)) {
                descendants.add(desc);
                dfs(desc, graph, descendants);
            }
        }
    }

    private ArrayList<Unit> getDescendants(Unit u, ExceptionalUnitGraph graph) {
        TreeSet<Unit> descendants = new TreeSet<>((Comparator<Unit>) (pta.unitcomparator));
        dfs(u, graph, descendants);
        return new ArrayList<Unit>(descendants);
    }

    boolean isInvokeExpression(Value expression) {
        return expression instanceof JVirtualInvokeExpr
                || expression instanceof JStaticInvokeExpr || expression instanceof JDynamicInvokeExpr;
    }

    private TreeSet<String> getEscapingVariables(Unit u, PTA.PointsToGraph goingIn) {
        TreeSet<String> escapingVars = new TreeSet<>();
        if (u instanceof JInvokeStmt) {
            JInvokeStmt stmt = (JInvokeStmt) (u);
            InvokeExpr expr = stmt.getInvokeExpr();
            if(isInvokeExpression(expr)){
                //to ignore specialinvoke
                handleEscapeFromInvokeExpr(goingIn, escapingVars, expr);
            }
            // any object passed as a parameter to a function call.
        } else if (u instanceof JAssignStmt) {
            JAssignStmt stmt = ((JAssignStmt) u);
            Value rhs;
            rhs = stmt.getRightOp();
            if (isInvokeExpression(rhs)) {
                AbstractInvokeExpr expr = (AbstractInvokeExpr) rhs;
                handleEscapeFromInvokeExpr(goingIn, escapingVars, expr);
            }
            // // assignment to parameter's fields.
            // // assignment to global vars.
            // if(lhs instanceof FieldRef){
            // //assignment to field of the enclosing class.
            // fillEscapingVarsFromExpression(goingIn, escapingVars, rhs, u);
            // }
            // else if(lhs instanceof JInstanceFieldRef){
            // //assignment to fields of globals or fields of params.
            // JInstanceFieldRef fref = (JInstanceFieldRef)(lhs);
            // Value base = fref.getBase();
            // if(base instanceof ParameterRef){
            // fillEscapingVarsFromExpression(goingIn, escapingVars, rhs, u);
            // }
            // }

        } else if (u instanceof JReturnStmt)

        {
            // returning any local objects.
            JReturnStmt ret = (JReturnStmt) (u);
            Local rhs = (Local) ret.getOp();
            addLocalToEscapingVars(goingIn, escapingVars, rhs);
        }
        return escapingVars;
    }

    private void handleEscapeFromInvokeExpr(PTA.PointsToGraph goingIn, TreeSet<String> escapingVars, InvokeExpr expr) {
        if (expr instanceof JVirtualInvokeExpr) {
            JVirtualInvokeExpr vexpr = (JVirtualInvokeExpr) (expr);
            Value base = vexpr.getBase();
            if (base instanceof Local) {
                Local local = (Local) (base);
                addLocalToEscapingVars(goingIn, escapingVars, local);
            }
        }
        for (int i = 0; i < expr.getArgCount(); i++) {
            Value arg = expr.getArg(i);
            if (arg instanceof Local) {
                Local local = (Local) arg;
                addLocalToEscapingVars(goingIn, escapingVars, local);
            }
        }
    }

    public void addLocalToEscapingVars(PTA.PointsToGraph goingIn, TreeSet<String> escapingVars, Local expr) {
        if (goingIn.stackMap.containsKey(expr.getName())) {
            escapingVars.addAll(goingIn.stackMap.get(expr.getName()));
        }
    }

}
