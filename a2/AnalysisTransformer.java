import java.util.*;

import soot.*;
// import soot.jimple.AnyNewExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JDynamicInvokeExpr;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;

// import soot.toolkits.graph.*;
// import soot.toolkits.scalar.BackwardFlowAnalysis;
// import soot.toolkits.scalar.FlowSet;
public class AnalysisTransformer extends BodyTransformer {
    class HeapReference {
        String object;
        String field;

        HeapReference() {
            object = "";
            field = "";
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof HeapReference)) {
                throw new RuntimeException("Bro why you comparing random objects?");
            }
            HeapReference hr = (HeapReference) (o);
            return hr.field.equals(this.field) && (hr.object.equals(this.object));
        }
    }

    class PointsToGraph {
        HashMap<String, Set<String>> stackMap;
        HashMap<HeapReference, Set<String>> heapMap;

        PointsToGraph() {
            stackMap = new HashMap<String, Set<String>>();
            heapMap = new HashMap<HeapReference, Set<String>>();
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
        Set<String> killStack;
        Set<HeapReference> killHeap;
        PointsToGraph gen;
    }

    class UnitComparator implements Comparator<Unit> {
        HashMap<Unit, Integer> units;

        UnitComparator(HashMap<Unit, Integer> _units) {
            this.units = _units;
        }

        @Override
        public int compare(Unit e1, Unit e2) {
            if (this.units.get(e1) < (this.units.get(e2))) {
                return -1;
            } else if (this.units.get(e1) > (this.units.get(e2))) {
                return 1;
            }
            return 0;
        }
    }

    PointsToGraph mergePredecessorData(List<Unit> units, HashMap<Unit, NodePointsToData> pointsToInfo) {
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
                merged.stackMap.put(stackVarName, pOut.stackMap.get(stackVarName));
            }
        }
        // combine all heapMap entries
        for (HeapReference heapRef : pOut.heapMap.keySet()) {
            if (merged.heapMap.containsKey(heapRef)) {
                // TODO: check this.
                merged.heapMap.get(heapRef).addAll(pOut.heapMap.get(heapRef));
            } else {
                merged.heapMap.put(heapRef, pOut.heapMap.get(heapRef));
            }
        }
    }

    KillGenSets getKillGenSets(PointsToGraph in, Unit u) {
        KillGenSets kgset = new KillGenSets();
        kgset.gen = new PointsToGraph();
        kgset.killHeap = new HashSet<HeapReference>();
        kgset.killStack = new HashSet<String>();
        if (u instanceof JAssignStmt) {
            JAssignStmt stmt = (JAssignStmt) u;
            Value rhs = stmt.getRightOp();
            Value lhs = stmt.getLeftOp();
            HashSet<String> newPointees = getNewPointees(in, u, rhs);
            fillKillGenSet(in, kgset, lhs, newPointees);
        }
        return kgset;
    }

    private void fillKillGenSet(PointsToGraph in, KillGenSets kgset, Value lhs, HashSet<String> newPointees) {
        if (lhs instanceof JInstanceFieldRef) {
            // field access=> update the heap, boy.
            JInstanceFieldRef fieldRef = (JInstanceFieldRef) (lhs);
            String field = fieldRef.getField().getName();
            String base = fieldRef.getBase().toString();
            if (in.stackMap.containsKey(base)) {
                Set<String> heapObjs = in.stackMap.get(base);
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
                // TODO: throw
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

    private HashSet<String> getNewPointees(PointsToGraph in, Unit u, Value rhs) {
        HashSet<String> newPointees = new HashSet<>();
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
                    Set<String> baseObjects = in.stackMap.get(base);
                    for (String baseObject : baseObjects) {
                        HeapReference hr = new HeapReference();
                        hr.field = field;
                        hr.object = baseObject;
                        if (in.heapMap.containsKey(hr)) {
                            newPointees.addAll(in.heapMap.get(hr));
                        } else {
                            // TODO: What
                            // may happen because dummy objects.
                            // or, uninitialized objects.
                        }
                    }
                } else {
                    // throw new RuntimeException(base+" not contained in in.stackMap!");
                }
            } else {
                if (!(isInvokeExpression(rhs))) {
                    String varName = rhs.toString();
                    if (in.stackMap.containsKey(varName)) {
                        newPointees.addAll(in.stackMap.get(varName));
                    }
                } else {
                    // TODO: What to do for calls?
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
                ans.stackMap.put(stackVarName, new HashSet<String>(in.stackMap.get(stackVarName)));
            }
        }
        for (HeapReference heapRef : in.heapMap.keySet()) {
            if (!kgsets.killHeap.contains(heapRef)) {
                ans.heapMap.put(heapRef, new HashSet<String>(in.heapMap.get(heapRef)));
            }
        }
        // add gen points-to info
        mergePointsToGraphs(ans, kgsets.gen);
        return ans;
    }

    PointsToGraph getDummyPointsToGraph(SootMethod method) {
        PointsToGraph g = new PointsToGraph();
        // TODO: implement
        return g;
    }

    void print(String s) {
        System.out.println(s);
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
         * Steps: 1. PTG for stack,heap. 2. Escape analysis using PTG 3. Print escaping
         * objects.
         */
        HashMap<Unit, NodePointsToData> pointsToInfo = new HashMap<Unit, NodePointsToData>();
        HashMap<Unit, Integer> unitIndices = new HashMap<Unit, Integer>();
        int index = 0;
        for (Unit u : units) {
            NodePointsToData ptd = new NodePointsToData();
            ptd.in = new PointsToGraph();
            ptd.out = new PointsToGraph();
            pointsToInfo.put(u, ptd);
            unitIndices.put(u, new Integer(index));
            index++;
        }
        UnitComparator comparator = new UnitComparator(unitIndices);
        SortedSet<Unit> workList = new TreeSet<Unit>(comparator);
        workList.add(units.getFirst());
        while (workList.size() > 0) {
            Unit u = workList.first();
            workList.remove(u);
            PointsToGraph newIn = new PointsToGraph();
            PointsToGraph newOut = new PointsToGraph();
            // process u:
            if (u == units.getFirst()) {
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
        for (Unit u : units) {
            print("Unit: \"" + u.toString() + "\"{");
            print("\tIn: ");
            pointsToInfo.get(u).in.print();
            print("\tOut: ");
            pointsToInfo.get(u).out.print();
            print("}");
        }
    }
}
