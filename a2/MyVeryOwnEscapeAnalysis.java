// import java.util.*;

import soot.*;
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

    public void doEscapeAnalysis(Body body,TreeMap<Unit, MyVeryOwnPointsToAnalysis.NodePointsToData> pointsToInfo) {
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
        TreeSet<Unit> workList = new TreeSet<Unit>();
        TreeSet<Unit> units = new TreeSet<>();
        HashMap<Unit,Integer> unitIndices = new HashMap<Unit,Integer>(); 
        for(Unit u:pointsToInfo.keySet()){
            unitIndices.put(u, units.size());
            units.add(u);
        }
        workList.add(units.first());
        while(workList.size()>0){
            Unit first = workList.first();
            workList.remove(first);
            
        }
    }
    
}
