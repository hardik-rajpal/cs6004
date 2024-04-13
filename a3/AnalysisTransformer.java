import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LiveLocals;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.util.Chain;


public class AnalysisTransformer extends SceneTransformer {
    static CallGraph cg;
    static TreeSet<String> results = new TreeSet<>();
    //SootMethod.toString()->{Escaping objects o1, o2, o3}
    TreeMap<String,Integer> collectionAfter = new TreeMap<>();
    TreeMap<String,LiveLocals> liveLocalsAnalysis = new TreeMap<>();
    static TreeSet<SootMethod> processedMethods = new TreeSet<>(new PTA.SootMethodComparator());
    static HashSet<SootMethod> userDefinedMethods = new HashSet<>();
    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {
        cg = Scene.v().getCallGraph();
        // Get the main method
        SootClass mainClass = Scene.v().getMainClass();
        Chain<SootClass> classes = Scene.v().getClasses();
        SootMethod mainMethod = null;
        for(SootClass classInstance:classes){
            //ignore java lang classes
            if(!classInstance.isApplicationClass()){
                continue;
            }
            List<SootMethod> methods = classInstance.getMethods();
            for(SootMethod method:methods){
                if(!method.isConstructor()){
                    if(method.isMain()&&classInstance.equals(mainClass)){
                        mainMethod = method;
                    }
                    userDefinedMethods.add(method);
                }
            }
        }
        if(mainMethod==null){
            throw new RuntimeException("No main method in testcase.");
        }
        // synchronized(System.out){
        // for(SootMethod method:userDefinedMethods){
        //     System.out.println("Method: "+method.toString());
        //     for(Unit unit:method.getActiveBody().getUnits()){
        //         if(PTA.isInvokeStmt(unit)){
        //             TreeSet<SootMethod> methods = PTA.getSootMethodsFromInvokeUnit(unit, cg);
        //             TreeSet<String> methodNames = new TreeSet<>();
        //             methods.forEach(calleeMethod->{methodNames.add(calleeMethod.toString());});
        //             System.out.println(unit.toString() + "calls: "+String.join(",", methodNames));
        //             }
        //         }
        //     }
        // }
        // for(SootMethod method:userDefinedMethods){
        //     processMethod(method);
        // }
        processMethod(mainMethod);
        for(SootMethod method:processedMethods){
            fillResult(method);
        }
    }
    public static class Range{
        int start;
        int end;
        Range(int _start,int _end){
            start = _start;
            end = _end;
        }
        public boolean contains(int line) {
            return start<=line && line <= end;
        }
    }
    private void fillResult(SootMethod method) {
        String ans = "";
        ans = method.getDeclaringClass().getName()+":"+method.getName()+" ";
        Range methodRange = getMethodRange(method);
        TreeSet<String> objectsCollectedHere = new TreeSet<>();
        for(String object:collectionAfter.keySet()){
            int line = collectionAfter.get(object);
            if(methodRange.contains(line)){
                objectsCollectedHere.add(object);
            }
        }
        ans += getOutputString(objectsCollectedHere);
        results.add(ans);
    }

    private Range getMethodRange(SootMethod method) {
        Body body = method.getActiveBody();
        int start = Integer.MAX_VALUE;
        int end = -1;
        for(Unit unit:body.getUnits()){
            if(unit.getJavaSourceStartLineNumber()<0){
                continue;
            }
            start = Integer.min(start,unit.getJavaSourceStartLineNumber());
            end = Integer.max(end,unit.getJavaSourceStartLineNumber());
        }
        return new Range(start, end);
    }

    private void processMethod(SootMethod method) {
        if(processedMethods.contains(method)){return;}
        fillAllObjectsGCLines(method);
        return;
    }
    public ArrayList<Local> getLiveLocalsAtTails(Body body){
        ArrayList<Local> ans = new ArrayList<>();
        BriefUnitGraph cfg = new BriefUnitGraph(body);
        //add all param locals.
        ans.addAll(body.getParameterLocals());
        //add all returned objects.
        for(Unit tail:cfg.getTails()){
            if(tail instanceof JReturnStmt){
                JReturnStmt stmt = (JReturnStmt)tail;
                Value returnValue = stmt.getOp();
                if(returnValue instanceof Local){
                    Local local = (Local)returnValue;
                    ans.add(local);
                }
            }
        }
        return ans;
    }
    private void fillAllObjectsGCLines(SootMethod method) {
        Body body = method.getActiveBody();
        UnitGraph cfg = new BriefUnitGraph(body);
        LiveLocals liveLocals = new SimpleLiveLocals(cfg);
        liveLocalsAnalysis.put(method.toString(), liveLocals);
        PTA pta = new PTA();
        TreeMap<String,String> paramMap = new TreeMap<>();
        PTA.CallerInfo callerInfo = new PTA.CallerInfo(new ArrayList<String>(), new PTA.PointsToGraph(), paramMap);
        callerInfo.userDefinedMethods = userDefinedMethods;
        callerInfo.cg = cg;
        TreeMap<Unit, PTA.NodePointsToData> pointsToInfo = pta.getPointsToInfo(body,callerInfo);
        // pta.printPointsToInfo(pointsToInfo);
        //mark all objects as dead.
        //Get line number after which object can be collected.
        List<Unit> tails = cfg.getTails();
        for(Unit unit:units){
            PTA.NodePointsToData info = pointsToInfo.get(unit);
            HashSet<Local> localsLiveBefore = new HashSet<>(liveLocals.getLiveLocalsBefore(unit));
            HashSet<Local> localsLiveAfter = new HashSet<>(liveLocals.getLiveLocalsAfter(unit));
            //add returned object to live vars after artificially:
            if(tails.contains(unit) && (unit instanceof JReturnStmt)){
                JReturnStmt stmt = (JReturnStmt)unit;
                Value returnValue = stmt.getOp();
                if(returnValue instanceof Local){
                    Local local = (Local)returnValue;
                    localsLiveAfter.add(local);
                }
            }
            TreeSet<String> newUncollectedObjects = new TreeSet<>();

            if(PTA.isInvokeStmt(unit)){
                TreeSet<String> liveObjectsAtCalleesTails = new TreeSet<>();
                InvokeExpr expr = PTA.getInvokeExprFromInvokeUnit(unit);
                TreeSet<SootMethod> calleeMethods = PTA.getSootMethodsFromInvokeUnit(unit, cg);
                if(userDefinedMethods.contains(expr.getMethod())){
                    for(SootMethod calleeMethod:calleeMethods){
                        if(!processedMethods.contains(calleeMethod)){
                            //termination guarranteed in the absence of recursion only.
                            processMethod(calleeMethod);
                        }
                    }
                    TreeSet<String> calleeEndLiveCallerLocalNames = new TreeSet<>();
                    if(unit instanceof JAssignStmt){
                        JAssignStmt stmt = (JAssignStmt)unit;
                        Local lhs = (Local)stmt.getLeftOp();
                        calleeEndLiveCallerLocalNames.add(lhs.getName());
                    }
                    else{
                        if(info.calleeReturnObjects!=null){
                            TreeSet<String> baseLiveObjects = info.calleeReturnObjects;
                            liveObjectsAtCalleesTails.addAll(baseLiveObjects);
                            for(String pointee:baseLiveObjects){
                                fillObjectsReachableFrom(pointee, liveObjectsAtCalleesTails, info.out.heapMap);
                            }
                        }
                    }
                    for(Value arg:expr.getArgs()){
                        if(arg instanceof Local){
                            Local local = (Local)arg;
                            calleeEndLiveCallerLocalNames.add(local.getName());
                        }
                    }
                    liveObjectsAtCalleesTails.addAll(getReachableObjects(info.out, calleeEndLiveCallerLocalNames));
                    newUncollectedObjects = liveObjectsAtCalleesTails;
                }
            }
            else{
                TreeSet<String> newObjects = new TreeSet<>(getAllObjects(info.out));
                newObjects.removeAll(getAllObjects(info.in));
                newUncollectedObjects = newObjects;
            }
            //all objects reachable from parameters are live=> set parameters as live, always.
            List<Local> paramLocals = body.getParameterLocals();
            //works even if params are reassigned to, since jimple creates new locals then.
            localsLiveAfter.addAll(paramLocals);
            TreeSet<String> objects =  getObjectsToCollect(localsLiveBefore,localsLiveAfter,info,newUncollectedObjects);
            int currLineNum = unit.getJavaSourceStartLineNumber();
            for(String object:objects){
                //don't collect dummy objects.
                if(!(object.contains("@"))){
                    int prev = -1;
                    String key = getCleanedObject(object);
                    if(collectionAfter.containsKey(key)){
                        prev = collectionAfter.get(key);
                    }
                    //max line num policy.
                    collectionAfter.put(key, Integer.max(prev,currLineNum));
                }
            }
        }
        processedMethods.add(method);
        return;
    }
    private String getOutputString(TreeSet<String> objects) {
        String ans = "";
        for(String obj:objects){
            // String cleanedObject = getCleanedObject(obj);
            ans += obj+":"+collectionAfter.get(obj)+" ";
        }
        return ans;
    }

    private String getCleanedObject(String obj) {
        String cleanedObject = obj;
        cleanedObject = cleanedObject.split(Pattern.quote("_"))[1];
        cleanedObject = cleanedObject.split(Pattern.quote("("))[0];
        return cleanedObject;
    }
    private void fillObjectsReachableFrom(String rv, TreeSet<String> reachableVars, TreeMap<PTA.HeapReference, TreeSet<String>> heapMap) {
        for (PTA.HeapReference hr : heapMap.keySet()) {
            if (hr.object.equals(rv)) {
                TreeSet<String> newRV = heapMap.get(hr);
                for (String newrv : newRV) {
                    if (!reachableVars.contains(newrv)) {
                        reachableVars.add(newrv);
                        fillObjectsReachableFrom(newrv, reachableVars, heapMap);
                    }
                }
            }
        }
    }
    private TreeSet<String> getReachableObjects(PTA.PointsToGraph in, Set<String> localsLiveBefore) {
        TreeSet<String> reachable = new TreeSet<>();
        for(String stackVar:localsLiveBefore){
            TreeSet<String> pointees = in.stackMap.get(stackVar);
            for(String pointee:pointees){
                reachable.add(pointee);
                fillObjectsReachableFrom(pointee, reachable, in.heapMap);
            }
        }
        return reachable;
    }

    private TreeSet<String> getObjectsToCollect(HashSet<Local> localsLiveBefore,
            HashSet<Local> localsLiveAfter, PTA.NodePointsToData info, TreeSet<String> newUncollectedObjects) {
        TreeSet<String> reachableBefore = getReachableObjects(info.in,getLocalNames(localsLiveBefore));
        TreeSet<String> reachableAfter = getReachableObjects(info.out, getLocalNames(localsLiveAfter));
        TreeSet<String> gcExistingObjects = new TreeSet<>(reachableBefore);
        gcExistingObjects.removeAll(reachableAfter);
        //now newObjects are all newObjects.
        TreeSet<String> gcNewObjects = new TreeSet<>(newUncollectedObjects);
        gcNewObjects.removeAll(reachableAfter);
        //remove collectedObjects

        TreeSet<String> allGcObjects = new TreeSet<>();
        allGcObjects.addAll(gcNewObjects);
        allGcObjects.addAll(gcExistingObjects);
        return allGcObjects;
    }
    // private void printSet(TreeSet<String> allObjects) {
    //     synchronized(System.out){
    //         for(String obj:allObjects){
    //             System.out.print(obj+", ");
    //         }
    //         System.out.println("");
    //     }
    // }

    private TreeSet<String> getAllObjects(PTA.PointsToGraph in) {
        TreeSet<String> ans = new TreeSet<>();
        for(String stackVar:in.stackMap.keySet()){
            ans.addAll(in.stackMap.get(stackVar));
        }
        for(PTA.HeapReference hr:in.heapMap.keySet()){
            ans.add(hr.object);
            ans.addAll(in.heapMap.get(hr));
        }
        return ans;
    }
}
