
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.PrimType;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
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
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JNewArrayExpr;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JNewMultiArrayExpr;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.Chain;

public class PTA {
    public static class CallerInfo {
        ArrayList<String> callSites;
        PointsToGraph ptgIn;
        TreeMap<String, String> paramMap;
        CallGraph cg;
        HashSet<SootMethod> userDefinedMethods;

        CallerInfo(ArrayList<String> _callSites, PointsToGraph _in, TreeMap<String, String> _paramMap) {
            callSites = _callSites;
            ptgIn = _in;
            paramMap = _paramMap;
        }

        String getContext() {
            return String.join(",", callSites);
        }
    }

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
        TreeSet<String> calleeReturnObjects;
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
        mergeStackGraphs(merged, pOut);
        mergeHeapGraphs(merged, pOut);
    }

    private static void mergeStackGraphs(PointsToGraph merged, PointsToGraph pOut) {
        for (String stackVarName : pOut.stackMap.keySet()) {
            if (merged.stackMap.containsKey(stackVarName)) {
                merged.stackMap.get(stackVarName).addAll(pOut.stackMap.get(stackVarName));
            } else {
                TreeSet<String> pointees = new TreeSet<>();
                pointees.addAll(pOut.stackMap.get(stackVarName));
                merged.stackMap.put(stackVarName, pointees);
            }
        }
    }

    private static void mergeHeapGraphs(PointsToGraph merged, PointsToGraph pOut) {
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

    private TreeSet<String> calleeReturnObjects;

    KillGenSets getKillGenSets(PointsToGraph in, Unit u) {
        KillGenSets kgset = new KillGenSets();
        kgset.gen = new PointsToGraph();
        kgset.killHeap = new TreeSet<HeapReference>(new HeapReferenceComparator());
        kgset.killStack = new TreeSet<String>();
        AbstractDefinitionStmt stmt;
        // accounting for JIdentityStmt
        // as that's used for @this aliasing.
        // That's handled by getDummyPointsToGraph
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
            // base==stackvar pointing to param0
            if (in.stackMap.containsKey(base)) {
                TreeSet<String> heapObjs = in.stackMap.get(base);
                if (heapObjs.size() == 1) {
                    // strong update
                    String s = heapObjs.first();
                    HeapReference hr = new HeapReference();
                    hr.field = field;
                    hr.object = s;
                    // strong update's kill.
                    // kgset.killHeap.add(hr);
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
        } else if (lhs instanceof StaticFieldRef) {
            StaticFieldRef sfref = (StaticFieldRef) (lhs);
            String stackVarName = sfref.getField().getName();
            // unconditional kill:
            kgset.killStack.add(stackVarName);
            kgset.gen.stackMap.put(stackVarName, newPointees);
        } else if (lhs instanceof Local) {
            Local local = (Local) lhs;
            String stackVarName = local.getName();
            // unconditional kill:
            kgset.killStack.add(stackVarName);
            kgset.gen.stackMap.put(stackVarName, newPointees);
        }
    }

    public static boolean isInvokeExpression(Value expression) {
        return expression instanceof JInterfaceInvokeExpr || expression instanceof JVirtualInvokeExpr
                || expression instanceof JStaticInvokeExpr || expression instanceof JDynamicInvokeExpr
                || expression instanceof JSpecialInvokeExpr;
    }

    private TreeSet<String> getNewPointees(PointsToGraph in, Unit u, Value rhs) {
        TreeSet<String> newPointees = new TreeSet<>();
        String ctxStr = callerInfo.getContext();
        if (rhs instanceof JNewExpr) {
            // allocation site abstraction:
            String objectName = "Obj_" + Integer.toString(u.getJavaSourceStartLineNumber()) + "(" + ctxStr + ")";
            newPointees.add(objectName);
        } else if (rhs instanceof JNewArrayExpr) {
            String objectName = "Obj_" + Integer.toString(u.getJavaSourceStartLineNumber()) + "(" + ctxStr + ")";
            newPointees.add(objectName);
        } else if (rhs instanceof JNewMultiArrayExpr) {
            String objectName = "Obj_" + Integer.toString(u.getJavaSourceStartLineNumber()) + "(" + ctxStr + ")";
            newPointees.add(objectName);

        } else if (rhs instanceof InvokeExpr) {
            String objName = "@Obj_" + Integer.toString(u.getJavaSourceStartLineNumber()) + "(" + ctxStr + ")";
            newPointees.add(objName);
        } else if (rhs instanceof JInstanceFieldRef) {
            // new pointees only if reftype
            JInstanceFieldRef fieldRef = (JInstanceFieldRef) (rhs);
            Type targetType = fieldRef.getType();
            String field = fieldRef.getField().getName();
            String base = fieldRef.getBase().toString();
            // must be non-primitive type.
            if ((!(targetType instanceof PrimType)) && in.stackMap.containsKey(base)) {
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
                        // targetType must be non-primitive and object must be dummy.
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
        } else if (rhs instanceof Local) {
            Local variable = (Local) (rhs);
            String varName = variable.getName();
            if (in.stackMap.containsKey(varName)) {
                newPointees.addAll(in.stackMap.get(varName));
            }
        } else if (rhs instanceof StaticFieldRef) {
            StaticFieldRef sfref = (StaticFieldRef) (rhs);
            String varName = sfref.getField().getName();
            if (in.stackMap.containsKey(varName)) {
                newPointees.addAll(in.stackMap.get(varName));
            }
        } else if (rhs instanceof ThisRef) {
            newPointees.add("@this");
        } else if (rhs instanceof ParameterRef) {
            // Fixed by dummy graph.
            // Just forward the defn.
            Local lhs = (Local) (((JIdentityStmt) u).getLeftOp());
            newPointees.add(in.stackMap.get(lhs.getName()).first());
        }
        return newPointees;
    }

    PointsToGraph flow(PointsToGraph in, Unit u, CallerInfo callerInfo) {
        PointsToGraph ans = new PointsToGraph();
        boolean useKgsets = true;
        if (isInvokeStmt(u)) {
            InvokeExpr expr = getInvokeExprFromInvokeUnit(u);
            TreeSet<SootMethod> methods = getSootMethodsFromInvokeUnit(u, callerInfo.cg);
            if (expr.getMethod().isConstructor() || (!callerInfo.userDefinedMethods.contains(expr.getMethod()))) {
                useKgsets = true;
            } else {
                // copy stackMap from in:
                ans.stackMap.putAll(in.stackMap);
                TreeMap<String, String> paramMap = new TreeMap<>();
                fillParamMap(paramMap, expr);
                String stackVarName = "";
                TreeSet<String> newPointees = new TreeSet<>();
                boolean assignStackVars = false;
                boolean getReturnVars = !(expr.getMethod().getReturnType() instanceof soot.VoidType);
                if (getReturnVars && (u instanceof JAssignStmt)) {
                    JAssignStmt stmt = (JAssignStmt) u;
                    Value lhs = stmt.getLeftOp();
                    // lhs will only be a local, because of jimple specs.
                    if (lhs instanceof Local) {
                        Local local = (Local) lhs;
                        assignStackVars = true;
                        stackVarName = local.getName();
                    }
                }
                for (SootMethod method : methods) {
                    Body body = method.getActiveBody();
                    ArrayList<String> newCallsiteList = new ArrayList<>();
                    newCallsiteList.addAll(callerInfo.callSites);
                    newCallsiteList.add(Integer.toString(u.getJavaSourceStartLineNumber()));
                    CallerInfo newCallerInfo = new CallerInfo(newCallsiteList, in, paramMap);
                    newCallerInfo.userDefinedMethods = callerInfo.userDefinedMethods;
                    newCallerInfo.cg = callerInfo.cg;
                    // isolate unit comparators.
                    PTA calleePTA = new PTA();
                    TreeMap<Unit, PTA.NodePointsToData> ptaInfo = calleePTA.getPointsToInfo(body, newCallerInfo);
                    PointsToGraph atCallEnd = mergePTGAtTails(body, ptaInfo);
                    // forward only heapMap from callee::
                    mergeHeapGraphs(ans, atCallEnd);
                    // add lhs var if any
                    if (getReturnVars) {
                        TreeSet<String> returnedObjects = getReturnObjectSet(body, ptaInfo);
                        newPointees.addAll(returnedObjects);
                    }
                }
                if(getReturnVars){
                    calleeReturnObjects = newPointees;
                }
                if(assignStackVars){
                    ans.stackMap.put(stackVarName, newPointees);
                }
                useKgsets = false;
            }
        }
        if (useKgsets) {
            KillGenSets kgsets = getKillGenSets(in, u);
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
            mergePointsToGraphs(ans, kgsets.gen);
        }
        // remove killed points-to info
        // add gen points-to info
        return ans;
    }

    private TreeSet<String> getReturnObjectSet(Body body, TreeMap<Unit, NodePointsToData> ptaInfo) {
        TreeSet<String> returned = new TreeSet<>();
        BriefUnitGraph cfg = new BriefUnitGraph(body);
        for (Unit u : cfg.getTails()) {
            if (u instanceof JReturnStmt) {
                JReturnStmt stmt = (JReturnStmt) u;
                Value returnValue = stmt.getOp();
                if (returnValue instanceof Local) {
                    Local local = (Local) returnValue;
                    String stackVar = local.getName();
                    NodePointsToData data = ptaInfo.get(u);
                    if (data.in.stackMap.containsKey(stackVar)) {
                        returned.addAll(data.in.stackMap.get(stackVar));
                    }
                } else {
                    // Possible with non-local return values.
                    // like: return 1;
                }
            } else {
                print("Non-return stmt found at tail of function used in assignment stmt!");
                System.exit(1);
            }
        }
        return returned;
    }

    private void fillParamMap(TreeMap<String, String> paramMap, InvokeExpr expr) {
        List<Local> params = expr.getMethod().getActiveBody().getParameterLocals();
        List<Value> args = expr.getArgs();

        for (int i = 0; i < params.size(); i++) {
            Local param = params.get(i);
            String key = param.getName();
            Value arg = args.get(i);
            // assume simple args like local1, local2, local3... because of jimple!
            if (arg instanceof Local) {
                Local obj = (Local) arg;
                String value = obj.getName();
                paramMap.put(key, value);
            }
        }
    }

    public static InvokeExpr getInvokeExprFromInvokeUnit(Unit u) {
        InvokeExpr ans;
        if (u instanceof JAssignStmt) {
            Value rhs = ((JAssignStmt) u).getRightOp();
            InvokeExpr expr = (InvokeExpr) rhs;
            ans = expr;
        } else {
            // u instanceof JInvokeStmt
            JInvokeStmt stmt = (JInvokeStmt) (u);
            InvokeExpr expr = stmt.getInvokeExpr();
            ans = expr;
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

    PointsToGraph getBoundaryInfoGraph(Body body, CallerInfo callerInfo) {
        PointsToGraph g = new PointsToGraph();
        // heap objects should be identifiable
        PointsToGraph info = callerInfo.ptgIn;

        for (HeapReference hr : info.heapMap.keySet()) {
            TreeSet<String> heapPointees = new TreeSet<>();
            heapPointees.addAll(info.heapMap.get(hr));
            g.heapMap.put(hr, heapPointees);
        }
        List<Local> params = body.getParameterLocals();
        // forward stackparamname
        for (int i = 0; i < params.size(); i++) {
            Local param = params.get(i);
            String calleeName = param.getName();
            if (callerInfo.paramMap.containsKey(calleeName)) {
                String callerName = callerInfo.paramMap.get(calleeName);
                TreeSet<String> pointees = new TreeSet<>();
                pointees.addAll(info.stackMap.get(callerName));
                g.stackMap.put(calleeName, pointees);
            }
        }
        return g;
    }

    PointsToGraph getDummyPointsToGraph(Body body) {
        PointsToGraph g = new PointsToGraph();
        SootMethod method = body.getMethod();
        // heap objects should be identifiable
        List<Local> params = body.getParameterLocals();
        for (int i = 0; i < params.size(); i++) {
            Local param = params.get(i);
            String stackParamName = param.getName();
            TreeSet<String> pointees = new TreeSet<>();
            String dummyObjName = "@param" + Integer.toString(i);
            pointees.add(dummyObjName);
            g.stackMap.put(stackParamName, pointees);
        }
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
        synchronized (System.out) {
            System.out.println(s);
        }
    }

    UnitComparator unitcomparator;
    CallerInfo callerInfo;

    public PointsToGraph mergePTGAtTails(Body body, TreeMap<Unit, NodePointsToData> ptinfo) {
        PointsToGraph ans = new PointsToGraph();
        BriefUnitGraph cfg = new BriefUnitGraph(body);
        for (Unit unit : cfg.getTails()) {
            mergePointsToGraphs(ans, ptinfo.get(unit).out);
        }
        return ans;
    }

    public TreeMap<Unit, NodePointsToData> getPointsToInfo(Body body, CallerInfo _callerInfo) {
        PatchingChain<Unit> units = body.getUnits();
        // Construct CFG for the current method's body
        callerInfo = _callerInfo;
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
        HashSet<Unit> seenOnce = new HashSet<>();
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
                if (callerInfo.callSites.size() > 0) {
                    newIn = getBoundaryInfoGraph(body, callerInfo);
                } else {
                    newIn = getDummyPointsToGraph(body);
                }
            } else {
                newIn = mergeOutsOf(graph.getPredsOf(u), pointsToInfo);
            }
            newOut = flow(newIn, u, callerInfo);
            // assign new in.
            NodePointsToData nodeData = pointsToInfo.get(u);
            nodeData.in = newIn;

            if(calleeReturnObjects!=null){
                nodeData.calleeReturnObjects = calleeReturnObjects;
                calleeReturnObjects = null;
            }

            // compare newOut with old Out
            if ((!seenOnce.contains(u))||(nodeData.out.hashCode() != newOut.hashCode())) {
                nodeData.out = newOut;
                workList.addAll(graph.getSuccsOf(u));
            }
            seenOnce.add(u);
        }
        return pointsToInfo;
    }

    void printPointsToInfo(TreeMap<Unit, NodePointsToData> pointsToInfo) {
        synchronized (System.out) {
            for (Unit u : pointsToInfo.keySet()) {
                print("Unit: \"" + u.toString() + "\"{");
                print("\tIn: ");
                pointsToInfo.get(u).in.print();
                print("\tOut: ");
                pointsToInfo.get(u).out.print();
                if(pointsToInfo.get(u).calleeReturnObjects!=null){
                    print("\tcalleeReturnedObjects: [");
                    String ans = "";
                    for(String obj:pointsToInfo.get(u).calleeReturnObjects){
                        ans += (obj+", ");
                    }
                    print(ans+"]");
                }
                print("}");
            }
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
