import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JDynamicInvokeExpr;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class Utils {
    public static boolean isInvokeExpression(Value expression) {
        return expression instanceof JInterfaceInvokeExpr || expression instanceof JVirtualInvokeExpr
                || expression instanceof JStaticInvokeExpr || expression instanceof JDynamicInvokeExpr
                || expression instanceof JSpecialInvokeExpr;
    }
    public static boolean isInvokeStmt(Unit u) {
        boolean ans = false;
        if (u instanceof JAssignStmt) {
            Value rhs = ((JAssignStmt) u).getRightOp();
            if (isInvokeExpression(rhs)) {
                ans = true;
            }
        } else if (u instanceof JInvokeStmt) {
            JInvokeStmt stmt = (JInvokeStmt) (u);
            InvokeExpr expr = stmt.getInvokeExpr();
            if (isInvokeExpression(expr)) {
                ans = true;
            }
        }
        return ans;
    }
    public static class SootMethodComparator implements Comparator<SootMethod>{

        @Override
        public int compare(SootMethod o1, SootMethod o2) {
            return o1.toString().compareTo(o2.toString());
        }
        
    }
    public static TreeSet<SootMethod> getSootMethodsFromInvokeUnit(Unit u, CallGraph cg) {
        TreeSet<SootMethod> ans = new TreeSet<>(new SootMethodComparator());
        for (Iterator<Edge> iter = cg.edgesOutOf(u); iter.hasNext();) {
            Edge edge = iter.next();
            SootMethod tgtMethod = edge.tgt();
            ans.add(tgtMethod);
        }
        return ans;
    }

}
