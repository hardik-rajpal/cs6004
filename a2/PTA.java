
//TODO: initialization to ->? for strong updates validity.
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.ThisRef;
import soot.jimple.internal.AbstractDefinitionStmt;
import soot.jimple.internal.JArrayRef;
// import soot.jimple.AnyNewExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JDynamicInvokeExpr;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JNewArrayExpr;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JNewMultiArrayExpr;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.Chain;

public class PTA {
    public static class HeapReference {
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
            int s1, s2;
            s1 = field.hashCode();
            s2 = object.hashCode();
            return s1 | (s2 << 1);
        }
    }

    public static class PointsToGraph {
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
            for (String stackVar : stackMap.keySet()) {
                stackString += stackVar;
                stackString += "{";
                for (String pointee : stackMap.get(stackVar)) {
                    stackString += pointee + ",";
                }
                stackString += "}";
            }
            String heapString = "";
            for (HeapReference heapVar : heapMap.keySet()) {
                heapString += heapVar.object + "." + heapVar.field;
                heapString += "{";
                for (String pointee : heapMap.get(heapVar)) {
                    heapString += pointee + ",";
                }
                heapString += "}";
            }
            code = stackString.hashCode() | (heapString.hashCode() << 1);
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
            Integer i1, i2;
            i1 = this.units.get(e1);
            i2 = this.units.get(e2);
            return i1.compareTo(i2);
        }
    }

    public static class HeapReferenceComparator implements Comparator<HeapReference> {

        @Override
        public int compare(HeapReference e1, HeapReference e2) {
            String s1, s2;
            s1 = e1.object + "." + e1.field;
            s2 = e2.object + "." + e2.field;
            return s1.compareTo(s2);
        }
    }

    public static PointsToGraph mergeOutsOf(List<Unit> units, TreeMap<Unit, NodePointsToData> pointsToInfo) {
        PointsToGraph merged = new PointsToGraph();
        for (Unit unit : units) {
            // predecessor out
            PointsToGraph pOut = pointsToInfo.get(unit).out;
            // merge points to graphs
            mergePointsToGraphs(merged, pOut);
        }
        return merged;
    }

    private static void mergePointsToGraphs(PointsToGraph merged, PointsToGraph pOut) {
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
        String unitstr = u.toString();
        if (unitstr.contains("parameter")) {
            System.out.println(unitstr);
            System.out.println(u.getClass());
        }
        AbstractDefinitionStmt stmt;
        // not accounting for JIdentityStmt
        // as that's only used for parameter aliasing.
        // That's handled by getDummyPointsToGraph
        if (u instanceof JAssignStmt) {
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
                    String s = heapObjs.first();
                    HeapReference hr = new HeapReference();
                    hr.field = field;
                    hr.object = s;
                    // strong update's kill.
                    kgset.killHeap.add(hr);
                    kgset.gen.heapMap.put(hr, newPointees);
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
        } else if (lhs instanceof JArrayRef) {
            JArrayRef arrayRef = (JArrayRef) (lhs);
            String field = "[]";
            String base = arrayRef.getBase().toString();
            if (in.stackMap.containsKey(base)) {
                // only weak updates allowed on arrays.
                TreeSet<String> heapObjs = in.stackMap.get(base);
                for (String s : heapObjs) {
                    HeapReference hr = new HeapReference();
                    hr.field = field;
                    hr.object = s;
                    kgset.gen.heapMap.put(hr, newPointees);
                }
            }
        } else {
            if (lhs instanceof StaticFieldRef) {
                StaticFieldRef sfref = (StaticFieldRef) (lhs);
                String stackVarName = sfref.getField().getName();
                // unconditional kill:
                kgset.killStack.add(stackVarName);
                kgset.gen.stackMap.put(stackVarName, newPointees);
            } else {
                String stackVarName = lhs.toString();
                // unconditional kill:
                kgset.killStack.add(stackVarName);
                kgset.gen.stackMap.put(stackVarName, newPointees);
            }
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
            //updates to inheap:
            JNewExpr expr = (JNewExpr)(rhs);
            Chain<SootField> fields = Scene.v().getSootClass(expr.getType().toString()).getFields();
            TreeSet<String> fieldStrs = new TreeSet<>();
            for(SootField field:fields){
                fieldStrs.add(field.getName());
            }
            initializeHeapMapForObject(in, objectName, fieldStrs);
            // System.exit(0);
        } else if (rhs instanceof JNewArrayExpr) {
            String objectName = "Obj_" + Integer.toString(u.getJavaSourceStartLineNumber());
            newPointees.add(objectName);
            TreeSet<String> fields = new TreeSet<String>();
            fields.add("[]");
            initializeHeapMapForObject(in, objectName, fields);
        } else if (rhs instanceof JNewMultiArrayExpr) {
            String objectName = "Obj_" + Integer.toString(u.getJavaSourceStartLineNumber());
            newPointees.add(objectName);
        } else if (rhs instanceof InvokeExpr) {
            String objName = "@Obj_" + Integer.toString(u.getJavaSourceStartLineNumber());

            newPointees.add(objName);
        } else if (rhs instanceof JInstanceFieldRef) {
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
                        if (baseObject.contains("@")) {
                            // dummy object
                            String objectName = "@Obj_" + Integer.toString(u.getJavaSourceStartLineNumber());
                            newPointees.add(objectName);
                            // update to heapmap:
                            TreeSet<String> dummyPointees = new TreeSet<>();
                            dummyPointees.add(objectName);
                            in.heapMap.put(hr, dummyPointees);
                        }
                    } else {
                    }
                }
            } else {
                // throw new RuntimeException(base+" not contained in in.stackMap!");
            }
        } else if (rhs instanceof JArrayRef) {
            JArrayRef arrayRef = (JArrayRef) (rhs);
            String field = "[]";
            String base = arrayRef.getBase().toString();
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
                        // assume null check analysis has been done.
                        // =>no null objects are used.
                        // =>must be dummy object (params)
                        if (baseObject.contains("@")) {
                            // dummy object
                            String objectName = "@Obj_" + Integer.toString(u.getJavaSourceStartLineNumber());
                            newPointees.add(objectName);
                            // update to heapmap:
                            TreeSet<String> dummyPointees = new TreeSet<>();
                            dummyPointees.add(objectName);
                            in.heapMap.put(hr, dummyPointees);
                        }
                    }
                }
            } else {
                // throw new RuntimeException(base+" not contained in in.stackMap!");
            }
        } else {
            if (rhs instanceof Local) {
                Local variable = (Local) (rhs);
                String varName = variable.getName();
                if (in.stackMap.containsKey(varName)) {
                    newPointees.addAll(in.stackMap.get(varName));
                }
            } else if (rhs instanceof ParameterRef) {
                // nop
                // because getDummyGraphtakes takes care of this.
            } else if (rhs instanceof StaticFieldRef) {
                // TODO: store info about obj being global
                StaticFieldRef sfref = (StaticFieldRef) (rhs);
                String varName = sfref.getField().getName();
                if (in.stackMap.containsKey(varName)) {
                    newPointees.addAll(in.stackMap.get(varName));
                }
            } else if (rhs instanceof ThisRef) {
                // what?
            }
        }
        return newPointees;
    }

    private void initializeHeapMapForObject(PointsToGraph in, String objectName, TreeSet<String> fields) {
        for(String field:fields){
            TreeSet<String> pointees = new TreeSet<>();
            pointees.add("?");
            HeapReference hr = new HeapReference();
            hr.field = field;
            hr.object = objectName;
            in.heapMap.put(hr, pointees);
        }
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

    PointsToGraph getInitPointsToGraph(Body body) {
        Chain<Local> locals = body.getLocals();
        PointsToGraph g = new PointsToGraph();
        SootMethod method = body.getMethod();
        // heap objects should be identifiable
        //locals init to ->?
        for(Local local:locals){
            TreeSet<String> pointees = new TreeSet<>();
            pointees.add("?");
            g.stackMap.put(local.getName(), pointees);
        }
        //params init to dummies
        List<Local> params = body.getParameterLocals();
        for (int i = 0; i < params.size(); i++) {
            Local param = params.get(i);
            String stackParamName = param.getName();
            TreeSet<String> pointees = new TreeSet<>();
            String dummyObjName = "@param" + Integer.toString(i);
            pointees.add(dummyObjName);
            g.stackMap.put(stackParamName, pointees);
        }
        //class fields init to dummies:
        Chain<SootField> fields = method.getDeclaringClass().getFields();
        for (SootField field : fields) {
            String stackParamName = field.getName();
            TreeSet<String> pointees = new TreeSet<>();
            String dummyObjName = "@field_" + stackParamName;
            pointees.add(dummyObjName);
            g.stackMap.put(stackParamName, pointees);

        }
        return g;
    }

    void print(String s) {
        System.out.println(s);
    }

    UnitComparator unitcomparator;

    public TreeMap<Unit, NodePointsToData> getPointsToInfo(Body body, String phaseName, Map<String, String> options) {
        PatchingChain<Unit> units = body.getUnits();
        // Construct CFG for the current method's body

        ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);

        HashMap<Unit, Integer> unitIndices = new HashMap<Unit, Integer>();
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
                // equality holds because no new unit objects are created.
                // equality in value must imply equality in reference.
                newIn = getInitPointsToGraph(body);
            } else {
                newIn = mergeOutsOf(graph.getPredsOf(u), pointsToInfo);
            }
            newOut = flow(newIn, u);
            // assign new in.
            NodePointsToData nodeData = pointsToInfo.get(u);
            nodeData.in = newIn;
            // compare newOut with old Out
            if (nodeData.out.hashCode() != newOut.hashCode()) {
                nodeData.out = newOut;
                workList.addAll(graph.getSuccsOf(u));
            }
        }
        return pointsToInfo;
    }

    void printPointsToInfo(TreeMap<Unit, NodePointsToData> pointsToInfo) {
        for (Unit u : pointsToInfo.keySet()) {
            print("Unit: \"" + u.toString() + "\"{");
            print("\tIn: ");
            pointsToInfo.get(u).in.print();
            print("\tOut: ");
            pointsToInfo.get(u).out.print();
            print("}");
        }
    }

    public TreeSet<String> getDummyHeapObjects(PointsToGraph goingIn) {
        TreeSet<String> dummies = new TreeSet<>();
        Set<String> stackvars = goingIn.stackMap.keySet();
        for (String svarname : stackvars) {
            dummies.addAll(goingIn.stackMap.get(svarname));
        }
        dummies.removeIf((s) -> {
            return (!s.contains("@"));
        });
        return dummies;

    }
}
