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
    class HeapReference{
        String object;
        String field;
    }
    class PointsToGraph{
        HashMap<String,Set<String>> stackMap;
        HashMap<HeapReference,Set<String>> heapMap;
        PointsToGraph(){
            stackMap = new HashMap<String,Set<String>>();
            heapMap = new HashMap<HeapReference,Set<String>>();
        }
    }
    class NodePointsToData{
        PointsToGraph in,out;
    }
    class KillGenSets{
        Set<String> killStack;
        Set<HeapReference> killHeap;
        PointsToGraph gen;
    }
    PointsToGraph mergePredecessorData(List<Unit> units,HashMap<Unit,NodePointsToData> pointsToInfo){
        PointsToGraph merged = new PointsToGraph();
        for(Unit unit:units){
            //predecessor out
            PointsToGraph pOut = pointsToInfo.get(unit).out;
            //merge points to graphs
            mergePointsToGraphs(merged, pOut);
        }
        return merged;
    }
    private void mergePointsToGraphs(PointsToGraph merged, PointsToGraph pOut) {
        for (String stackVarName:pOut.stackMap.keySet()){
            if(merged.stackMap.containsKey(stackVarName)){
                merged.stackMap.get(stackVarName).addAll(pOut.stackMap.get(stackVarName));
            }
            else{
                merged.stackMap.put(stackVarName, pOut.stackMap.get(stackVarName));
            }
        }
        //combine all heapMap entries
        for(HeapReference heapRef:pOut.heapMap.keySet()){
            if(merged.heapMap.containsKey(heapRef)){
                //TODO: check this.
                merged.heapMap.get(heapRef).addAll(pOut.heapMap.get(heapRef));
            }
            else{
                merged.heapMap.put(heapRef,pOut.heapMap.get(heapRef));
            }
        }
    }
    KillGenSets getKillGenSets(PointsToGraph in, Unit u, HashMap<Unit,NodePointsToData> pointsToInfo){
        KillGenSets kgset = new KillGenSets();
        kgset.gen = new PointsToGraph();
        kgset.killHeap = new HashSet<HeapReference>();
        kgset.killStack = new HashSet<String>();
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
        return kgset;
    }
    PointsToGraph flow(PointsToGraph in, Unit u, HashMap<Unit,NodePointsToData> pointsToInfo){
        PointsToGraph ans = new PointsToGraph();
        KillGenSets kgsets = getKillGenSets(in, u, pointsToInfo);
        //remove killed points-to info
        for(String stackVarName:in.stackMap.keySet()){
            if(!kgsets.killStack.contains(stackVarName)){
                ans.stackMap.put(stackVarName, new HashSet<String>(in.stackMap.get(stackVarName)));
            }
        }
        for(HeapReference heapRef:in.heapMap.keySet()){
            if(!kgsets.killHeap.contains(heapRef)){
                ans.heapMap.put(heapRef, new HashSet<String>(in.heapMap.get(heapRef)));
            }
        }
        //add gen points-to info
        mergePointsToGraphs(ans, kgsets.gen);
        return ans;
    }
    PointsToGraph getDummyPointsToGraph(SootMethod method){
        PointsToGraph g = new PointsToGraph();
        return g;
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
        SortedSet<Unit> workList = new TreeSet<Unit>();
        HashMap<Unit,NodePointsToData> pointsToInfo = new HashMap<Unit,NodePointsToData>();
        for(Unit u:units){
            NodePointsToData ptd = new NodePointsToData();
            ptd.in = new PointsToGraph();
            ptd.out = new PointsToGraph();
            pointsToInfo.put(u, ptd);
            // System.out.println("current unit: "+u.toString());
            // List<Unit> preds = graph.getSuccsOf(u);
            // System.out.println("Succs{");
            // for(Unit b:preds){
            //     System.out.println("\t"+b.toString());
            // }
            // System.out.println("}End Preds");
        }
        workList.add(units.getFirst());
        while (workList.size()>0) {
            Unit u = workList.first();
            workList.remove(u);
            PointsToGraph newIn = new PointsToGraph();
            PointsToGraph newOut = new PointsToGraph();
            //process u:
            if(u==units.getFirst()){
                newIn = getDummyPointsToGraph(body.getMethod());
            }
            else{
                newIn = mergePredecessorData(graph.getPredsOf(u),pointsToInfo);
            }
            newOut = flow(newIn,u,pointsToInfo);
            //assign new in.
            NodePointsToData nodeData = pointsToInfo.get(u);
            nodeData.in = newIn;
            //compare newOut with oldOut
            if(nodeData.out.hashCode()!=newOut.hashCode()){
                nodeData.out = newOut;
                workList.addAll(graph.getSuccsOf(u));
            }
        }
    }
}
