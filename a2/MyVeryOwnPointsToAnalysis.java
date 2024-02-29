import java.util.*;

import soot.*;
import soot.jimple.internal.AbstractDefinitionStmt;
// import soot.jimple.AnyNewExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JDynamicInvokeExpr;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class MyVeryOwnPointsToAnalysis {
    class HeapReference {
        String object;
        String field;
        boolean isDummy = false;
        HeapReference() {
            object = "";
            field = "";
        }
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof HeapReference)) {
                throw new RuntimeException("Bro why are you comparing random objects?");
            }
            HeapReference hr = (HeapReference) (o);
            return hr.field.equals(this.field) && (hr.object.equals(this.object));
        }
        @Override
        public int hashCode() {
            int s1,s2;
            s1 = field.hashCode();
            s2 = object.hashCode();
            return s1|(s2<<1);
        }
    }

    class PointsToGraph {
        TreeMap<String, TreeSet<String>> stackMap;
        TreeMap<HeapReference, TreeSet<String>> heapMap;
        PointsToGraph() {
            // hrcomp 
            stackMap = new TreeMap<String, TreeSet<String>>();
            heapMap = new TreeMap<HeapReference, TreeSet<String>>(new HeapReferenceComparator());
        }
        @Override
        public int hashCode() {
            int code = 0;
            String stackString = "";
            for(String stackVar:stackMap.keySet()){
                stackString+=stackVar;
                stackString+="{";
                for(String pointee:stackMap.get(stackVar)){
                    stackString+=pointee+",";
                }
                stackString+="}";
            }
            String heapString = "";
            for(HeapReference heapVar:heapMap.keySet()){
                heapString+=heapVar.object+"."+heapVar.field;
                heapString+="{";
                for(String pointee:heapMap.get(heapVar)){
                    heapString+=pointee+",";
                }
                heapString+="}";
            }
            code = stackString.hashCode()|(heapString.hashCode()<<1);
            return code;
        }
        void print(String s) {
            System.out.print(s);
        }

        void print() {
            print("\t{\n");
            print("\t\tstack{\n");
            if (stackMap.size() > 0) {
                for (String stackVarName : stackMap.keySet()) {
                    print("\t\t\t" + stackVarName + "->[ ");
                    for (String pointee : stackMap.get(stackVarName)) {
                        print(pointee + ", ");
                    }
                    print("]\n");
                }
            }
            print("\t\t}\n");
            print("\t\theap{\n");
            if (heapMap.size() > 0) {
                for (HeapReference hr : heapMap.keySet()) {
                    print("\t\t\t" + hr.object + "." + hr.field + "->[ ");
                    for (String pointee : heapMap.get(hr)) {
                        print(pointee + ", ");
                    }
                    print("]\n");
                }
            }
            print("\t\t}\n");
            print("\t}\n");
        }
    }

    class NodePointsToData {
        PointsToGraph in, out;
    }

    class KillGenSets {
        TreeSet<String> killStack;
        TreeSet<HeapReference> killHeap;
        PointsToGraph gen;
    }

    class UnitComparator implements Comparator<Unit> {
        HashMap<Unit, Integer> units;

        UnitComparator(HashMap<Unit, Integer> _units) {
            this.units = _units;
        }

        @Override
        public int compare(Unit e1, Unit e2) {
            Integer i1,i2;
            i1 = this.units.get(e1);
            i2 = this.units.get(e2);
            return i1.compareTo(i2);
        }
    }

    class HeapReferenceComparator implements Comparator<HeapReference> {

        @Override
        public int compare(HeapReference e1, HeapReference e2) {
            String s1,s2;
            s1 = e1.object+"."+e1.field;
            s2 = e2.object+"."+e2.field;
            return s1.compareTo(s2);
        }
    }

    PointsToGraph mergePredecessorData(List<Unit> units, TreeMap<Unit, NodePointsToData> pointsToInfo) {
        PointsToGraph merged = new PointsToGraph();
        for (Unit unit : units) {
            // predecessor out
            PointsToGraph pOut = pointsToInfo.get(unit).out;
            // merge points to graphs
            mergePointsToGraphs(merged, pOut);
        }
        return merged;
    }

    private void mergePointsToGraphs(PointsToGraph merged, PointsToGraph pOut) {
        for (String stackVarName : pOut.stackMap.keySet()) {
            if (merged.stackMap.containsKey(stackVarName)) {
                merged.stackMap.get(stackVarName).addAll(pOut.stackMap.get(stackVarName));
            } else {
                TreeSet<String> pointees = new TreeSet<>();
                pointees.addAll(pOut.stackMap.get(stackVarName));
                merged.stackMap.put(stackVarName, pointees);
            }
        }
        // combine all heapMap entries
        for (HeapReference heapRef : pOut.heapMap.keySet()) {
            if (merged.heapMap.containsKey(heapRef)) {
                merged.heapMap.get(heapRef).addAll(pOut.heapMap.get(heapRef));
            } else {
                TreeSet<String> pointees = new TreeSet<>();
                pointees.addAll(pOut.heapMap.get(heapRef));
                merged.heapMap.put(heapRef, pointees);
            }
        }
    }

    KillGenSets getKillGenSets(PointsToGraph in, Unit u) {
        KillGenSets kgset = new KillGenSets();
        kgset.gen = new PointsToGraph();
        kgset.killHeap = new TreeSet<HeapReference>(new HeapReferenceComparator());
        kgset.killStack = new TreeSet<String>();
        String unitstr  = u.toString();
        if(unitstr.contains("parameter")){
            System.out.println(unitstr);
            System.out.println(u.getClass());
        }
        AbstractDefinitionStmt stmt;
        if (u instanceof JAssignStmt || u instanceof JIdentityStmt) {
            stmt = (AbstractDefinitionStmt) u;
            Value rhs = stmt.getRightOp();
            Value lhs = stmt.getLeftOp();
            TreeSet<String> newPointees = getNewPointees(in, u, rhs);
            fillKillGenSet(in, kgset, lhs, newPointees);
        }
        return kgset;
    }

    private void fillKillGenSet(PointsToGraph in, KillGenSets kgset, Value lhs, TreeSet<String> newPointees) {
        if (lhs instanceof JInstanceFieldRef) {
            // field access=> update the heap, boy.
            JInstanceFieldRef fieldRef = (JInstanceFieldRef) (lhs);
            String field = fieldRef.getField().getName();
            String base = fieldRef.getBase().toString();
            if (in.stackMap.containsKey(base)) {
                TreeSet<String> heapObjs = in.stackMap.get(base);
                if (heapObjs.size() == 1) {
                    // strong update
                    for (String s : heapObjs) {
                        HeapReference hr = new HeapReference();
                        hr.field = field;
                        hr.object = s;
                        kgset.killHeap.add(hr);
                        kgset.gen.heapMap.put(hr, newPointees);
                    }
                } else {
                    // weak update, always done.
                    for (String s : heapObjs) {
                        HeapReference hr = new HeapReference();
                        hr.field = field;
                        hr.object = s;
                        kgset.gen.heapMap.put(hr, newPointees);
                    }
                }
            } else {
                
            }
        } else {
            String stackVarName = lhs.toString();
            // unconditional kill:
            kgset.killStack.add(stackVarName);
            kgset.gen.stackMap.put(stackVarName, newPointees);
        }
    }

    boolean isInvokeExpression(Value expression) {
        return expression instanceof JInterfaceInvokeExpr || expression instanceof JVirtualInvokeExpr
                || expression instanceof JStaticInvokeExpr || expression instanceof JDynamicInvokeExpr
                || expression instanceof JSpecialInvokeExpr;
    }

    private TreeSet<String> getNewPointees(PointsToGraph in, Unit u, Value rhs) {
        TreeSet<String> newPointees = new TreeSet<>();
        if (rhs instanceof JNewExpr) {
            // allocation site abstraction:
            String objectName = "Obj_" + Integer.toString(u.getJavaSourceStartLineNumber());
            newPointees.add(objectName);
        } else {
            // rhs.getType() instanceof soot.RefType
            if (rhs instanceof JInstanceFieldRef) {
                // new pointees only if reftype
                JInstanceFieldRef fieldRef = (JInstanceFieldRef) (rhs);
                String field = fieldRef.getField().getName();
                String base = fieldRef.getBase().toString();
                if (in.stackMap.containsKey(base)) {
                    TreeSet<String> baseObjects = in.stackMap.get(base);
                    for (String baseObject : baseObjects) {
                        HeapReference hr = new HeapReference();
                        hr.field = field;
                        hr.object = baseObject;
                        if (in.heapMap.containsKey(hr)) {
                            newPointees.addAll(in.heapMap.get(hr));
                        } else {
                            // may happen because dummy objects.
                            //assume null check analysis has been done.
                            //=>no null objects are used.
                            //=>must be dummy object (params)
                            //regardless, gen is phi.
                            // newpointees.add(phi).
                        }
                    }
                } else {
                    // throw new RuntimeException(base+" not contained in in.stackMap!");
                }
            } else {
                if (!(isInvokeExpression(rhs))) {
                    String varName = rhs.toString();
                    if(varName.contains(":") && varName.contains("parameter")){
                        varName = varName.split(":")[0];
                    }
                    if (in.stackMap.containsKey(varName)) {
                        newPointees.addAll(in.stackMap.get(varName));
                    }
                } else {
                    // ignoring calls
                }
            }
        }
        return newPointees;
    }

    PointsToGraph flow(PointsToGraph in, Unit u) {
        PointsToGraph ans = new PointsToGraph();
        KillGenSets kgsets = getKillGenSets(in, u);
        // remove killed points-to info
        for (String stackVarName : in.stackMap.keySet()) {
            if (!kgsets.killStack.contains(stackVarName)) {
                ans.stackMap.put(stackVarName, new TreeSet<String>(in.stackMap.get(stackVarName)));
            }
        }
        for (HeapReference heapRef : in.heapMap.keySet()) {
            if (!kgsets.killHeap.contains(heapRef)) {
                ans.heapMap.put(heapRef, new TreeSet<String>(in.heapMap.get(heapRef)));
            }
        }
        // add gen points-to info
        mergePointsToGraphs(ans, kgsets.gen);
        return ans;
    }

    PointsToGraph getDummyPointsToGraph(SootMethod method) {
        PointsToGraph g = new PointsToGraph();
        for(int i=0;i<method.getParameterCount();i++){
            String stackParamName = "@parameter"+Integer.toString(i);
            String dummyObjName = "param_"+Integer.toString(i);
            TreeSet<String> pointees = new TreeSet<>();
            pointees.add(dummyObjName);
            g.stackMap.put(stackParamName, pointees);
        }
        return g;
    }

    void print(String s) {
        System.out.println(s);
    }
    public TreeMap<Unit, NodePointsToData> getPointsToInfo(Body body, String phaseName, Map<String, String> options){
        PatchingChain<Unit> units = body.getUnits();
        // Construct CFG for the current method's body

        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
        
        HashMap<Unit, Integer> unitIndices = new HashMap<Unit, Integer>();
        UnitComparator unitcomparator;
        unitcomparator = new UnitComparator(unitIndices);
        TreeMap<Unit, NodePointsToData> pointsToInfo = new TreeMap<Unit, NodePointsToData>(unitcomparator);
        int index = 0;
        for (Unit u : units) {
            unitIndices.put(u, new Integer(index));
            NodePointsToData ptd = new NodePointsToData();
            ptd.in = new PointsToGraph();
            ptd.out = new PointsToGraph();
            pointsToInfo.put(u, ptd);
            index++;
        }
        SortedSet<Unit> workList = new TreeSet<Unit>(unitcomparator);
        Unit first = units.getFirst();
        workList.add(first);
        while (workList.size() > 0) {
            Unit u = workList.first();
            workList.remove(u);
            PointsToGraph newIn = new PointsToGraph();
            PointsToGraph newOut = new PointsToGraph();
            // process u:
            if (u == first) {
                newIn = getDummyPointsToGraph(body.getMethod());
            } else {
                newIn = mergePredecessorData(graph.getPredsOf(u), pointsToInfo);
            }
            newOut = flow(newIn, u);
            // assign new in.
            NodePointsToData nodeData = pointsToInfo.get(u);
            nodeData.in = newIn;
            // compare newOut with oldOut
            if (nodeData.out.hashCode() != newOut.hashCode()) {
                nodeData.out = newOut;
                workList.addAll(graph.getSuccsOf(u));
            }
        }
        return pointsToInfo;
    }
    void printPointsToInfo(TreeMap<Unit, NodePointsToData> pointsToInfo){
        for(Unit u:pointsToInfo.keySet()){
            print("Unit: \"" + u.toString() + "\"{");
            print("\tIn: ");
            pointsToInfo.get(u).in.print();
            print("\tOut: ");
            pointsToInfo.get(u).out.print();
            print("}");
        }
    }
}
