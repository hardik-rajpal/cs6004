// import java.util.*;

import soot.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JDynamicInvokeExpr;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
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

import java.util.*;


public class MyVeryOwnEscapeAnalysis {
    public void doEscapeAnalysis(Body body,TreeMap<Unit, MyVeryOwnPointsToAnalysis.NodePointsToData> pointsToInfo,MyVeryOwnPointsToAnalysis pta) {
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
        TreeMap<Unit,TreeSet<String>> escapingVariables = new TreeMap<>();
        Unit[] units = (Unit[])pointsToInfo.keySet().toArray();
        for(Unit u:units){
            MyVeryOwnPointsToAnalysis.PointsToGraph goingIn = pointsToInfo.get(u).in;
            escapingVariables.put(u, getEscapingVariables(u,pointsToInfo));
            MyVeryOwnPointsToAnalysis.PointsToGraph allSuccPTGs = mergeAllSuccPTGs(u,pointsToInfo,graph);
        }
    }

    private MyVeryOwnPointsToAnalysis.PointsToGraph mergeAllSuccPTGs(Unit u,
            TreeMap<Unit, MyVeryOwnPointsToAnalysis.NodePointsToData> pointsToInfo, ExceptionalUnitGraph graph) {
                MyVeryOwnPointsToAnalysis.PointsToGraph ans = new MyVeryOwnPointsToAnalysis.PointsToGraph();
                int oldHashcode;
                do{
                    oldHashcode = ans.hashCode();
                    //TODO:
                }
                while(oldHashcode!=ans.hashCode());
                return ans;
                
    }
    boolean isInvokeExpression(Value expression) {
        return expression instanceof JInterfaceInvokeExpr || expression instanceof JVirtualInvokeExpr
                || expression instanceof JStaticInvokeExpr || expression instanceof JDynamicInvokeExpr
                || expression instanceof JSpecialInvokeExpr;
    }

    private TreeSet<String> getEscapingVariables(Unit u,
            TreeMap<Unit, MyVeryOwnPointsToAnalysis.NodePointsToData> pointsToInfo) {
        TreeSet<String> escapingVars = new TreeSet<>();
        if(u instanceof JInvokeStmt){
            //any object passed as a parameter to a function call.
        }
        else if(u instanceof JAssignStmt){
            //assignment to parameter's fields. 
            //assignment to global vars.
        }
        else if(u instanceof JReturnStmt){
            //returning any local objects.
        }
        return escapingVars;
    }
    
}
