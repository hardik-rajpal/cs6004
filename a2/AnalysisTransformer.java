import java.util.*;
import soot.*;
// import soot.jimple.AnyNewExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JNewExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;
// import soot.toolkits.graph.*;
// import soot.toolkits.scalar.BackwardFlowAnalysis;
// import soot.toolkits.scalar.FlowSet;
public class AnalysisTransformer extends BodyTransformer {
    public class HeapReference{
        String object;
        String field;
    }
    public class PointsToGraph{
        HashMap<String,String> inStackMap;
        HashMap<HeapReference,String> inHeapMap;
        PointsToGraph(){
            inStackMap = new HashMap<String,String>();
            inHeapMap = new HashMap<HeapReference,String>();
        }
    }
    public class NodePointsToData{
        PointsToGraph in,out;
    }
    PointsToGraph mergePredecessorData(List<UnitBox> boxes){
        return new PointsToGraph();
    }
    PointsToGraph flow(PointsToGraph in, Unit u){
        PointsToGraph ans = new PointsToGraph();
        if(u instanceof JAssignStmt){
            //do smt.
            JAssignStmt stmt = (JAssignStmt)u;
            Value rhs = stmt.getRightOp();
            if(rhs instanceof JNewExpr){
                //do smt.
            }
        }
        else{
            // other types of statements lol.
        }
        return ans;
    }
    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        // Construct CFG for the current method's body
        PatchingChain<Unit> units = body.getUnits();
        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
        // String methodName = body.getMethod().getName();
        // Iterate over each unit of CFG.
        // Shown how to get the line numbers if the unit is a "new" Statement.
        /*
         Steps:
         1. PTG for stack,heap.
         2. 
         */
        ArrayList<Unit> workList = new ArrayList<>();
        HashMap<Unit,NodePointsToData> pointsToInfo = new HashMap<Unit,NodePointsToData>();
        for(Unit u:units){
            NodePointsToData ptd = new NodePointsToData();
            ptd.in = new PointsToGraph();
            ptd.out = new PointsToGraph();
            pointsToInfo.put(u, ptd);
            System.out.println("current unit: "+u.toString());
            List<Unit> preds = graph.getSuccsOf(u);
            System.out.println("Preds{");
            for(Unit b:preds){
                System.out.println("\t"+b.toString());
            }
            System.out.println("}End Preds");
        }
        workList.add(units.getFirst());
        // while (workList.size()>0) {
        //     Unit u = workList.get(0);
        //     workList.remove(0);
        //     PointsToGraph newIn = new PointsToGraph();
        //     PointsToGraph newOut = new PointsToGraph();

        //     //process u:
        //     newIn = mergePredecessorData(u.getBoxesPointingToThis());
        //     newOut = flow(newIn,u);

        //     if(pointsToInfo.containsKey(u)){
        //         pointsToInfo.get(u).in = newIn;
        //         NodePointsToData nodeData = pointsToInfo.get(u);
        //         if(nodeData.out.hashCode()!=newOut.hashCode()){
        //             nodeData.out = newOut;
        //             //TODO: add successors:

        //         }
        //     }
        //     else{
        //         NodePointsToData ptd = new NodePointsToData();
        //         ptd.in = newIn;
        //         ptd.out = newOut;
        //         pointsToInfo.put(u, ptd);
        //     }
        //     // if (u instanceof JAssignStmt) {
        //     //     JAssignStmt stmt = (JAssignStmt) u;
        //     //     Value rhs = stmt.getRightOp();
        //     //     if (rhs instanceof JNewExpr) {
        //     //         try {
        //     //             System.out.println("Unit is " + u + " and the line number is : " + u.getJavaSourceStartLineNumber());
        //     //         } catch (Exception e) {
        //     //             System.out.println("Unit is " + u + " and the line number is : " + -1);
        //     //         }
        //     //     }
        //     // }
        // }


    }
}
