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
    static TreeSet<String> processedMethods = new TreeSet<>();
    static HashSet<SootMethod> userDefinedMethods = new HashSet<>();
    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {
        cg = Scene.v().getCallGraph();
        // Get the main method
        Chain<SootClass> classes = Scene.v().getClasses();
        for(SootClass classInstance:classes){
            //ignore java lang classes
            if(!classInstance.isApplicationClass()){
                continue;
            }
            //TODO: virtual object calls, consider all methods.
            List<SootMethod> methods = classInstance.getMethods();
            for(SootMethod method:methods){
                if(!method.isConstructor()){
                    userDefinedMethods.add(method);
                }
            }
        }
        for(SootMethod method:userDefinedMethods){
            processMethod(method);
        }
        for(SootMethod method:userDefinedMethods){
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
        BriefUnitGraph cfg = new BriefUnitGraph(body);
        int start = Integer.MAX_VALUE;
        int end = -1;
        for(Unit unit:cfg.getHeads()){
            start = Integer.min(start,unit.getJavaSourceStartLineNumber());
        }
        for(Unit unit:cfg.getTails()){
            end = Integer.max(end,unit.getJavaSourceStartLineNumber());
        }
        return new Range(start, end);
    }

    private void processMethod(SootMethod method) {
        if(processedMethods.contains(method.toString())){return;}
        fillAllObjectsGCLines(method);
        return;
    }

    private void fillAllObjectsGCLines(SootMethod method) {
        Body body = method.getActiveBody();
        UnitPatchingChain units = body.getUnits();
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
            TreeSet<String> calleeTailObjects = new TreeSet<>();
            if(PTA.isInvokeStmt(unit)){
                TreeSet<SootMethod> calleeMethods = PTA.getSootMethodsFromInvokeUnit(unit, cg);
                if(userDefinedMethods.contains(calleeMethods.first())){
                    for(SootMethod calleeMethod:calleeMethods){
                        String calleeMethodKey = calleeMethod.toString();
                        if(!processedMethods.contains(calleeMethodKey)){
                            //termination guarranteed in the absence of recursion only.
                            processMethod(calleeMethod);
                        }
                        //some union over return value reachable objects to be subtracted from.
                        BriefUnitGraph calleeCFG = new BriefUnitGraph(calleeMethod.getActiveBody());
                        for(Unit tail:calleeCFG.getTails()){
                            List<Local> tailLocals = liveLocalsAnalysis.get(calleeMethodKey).getLiveLocalsAfter(tail);
                        };
                        // calleeTailObjects.addAll();
                    }
                }
            }
            //all objects reachable from parameters are live=> set parameters as live, always.
            List<Local> paramLocals = body.getParameterLocals();
            //works even if params are reassigned to, since jimple creates new locals then.
            localsLiveAfter.addAll(paramLocals);
            TreeSet<String> objects =  getObjectsToCollect(localsLiveBefore,localsLiveAfter,info,calleeTailObjects);
            int currLineNum = unit.getJavaSourceStartLineNumber();
            for(String object:objects){
                //don't collect dummy objects.
                if(!(object.contains("@"))){
                    int prev = -1;
                    if(collectionAfter.containsKey(object)){
                        prev = collectionAfter.get(object);
                    }
                    //max line num policy.
                    collectionAfter.put(object, Integer.max(prev,currLineNum));
                }
            }
        }
        processedMethods.add(method.toString());
        return;
    }
    private String getOutputString(TreeSet<String> objects) {
        String ans = "";
        for(String obj:objects){
            String cleanedObject = obj;
            cleanedObject = cleanedObject.split(Pattern.quote("_"))[1];
            cleanedObject = cleanedObject.split(Pattern.quote("("))[0];
            ans += cleanedObject+":"+collectionAfter.get(obj)+" ";
        }
        return ans;
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
            HashSet<Local> localsLiveAfter, PTA.NodePointsToData info, TreeSet<String> calleeTailObjects) {
        TreeSet<String> reachableBefore = getReachableObjects(info.in,getLocalNames(localsLiveBefore));
        TreeSet<String> reachableAfter = getReachableObjects(info.out, getLocalNames(localsLiveAfter));
        TreeSet<String> gcExistingObjects = new TreeSet<>(reachableBefore);
        gcExistingObjects.removeAll(reachableAfter);

        TreeSet<String> newObjects = new TreeSet<>(getAllObjects(info.out));
        newObjects.removeAll(getAllObjects(info.in));
        //now newObjects are all newObjects.
        TreeSet<String> gcNewObjects = new TreeSet<>(newObjects);
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

    private Set<String> getLocalNames(HashSet<Local> localsLiveAfter) {
        TreeSet<String> ans = new TreeSet<>();
        for(Local local:localsLiveAfter){
            ans.add(local.getName());
        }
        return ans;
    }

    //     // Iterate over the edges originating from this method
    //     Iterator<Edge> edges = Scene.v().getCallGraph().edgesOutOf(method);
    //     while (edges.hasNext()) {
    //         Edge edge = edges.next();
    //         SootMethod targetMethod = edge.tgt();
    //         // Recursively explore callee methods
    //         if (!targetMethod.isJavaLibraryMethod()) {
    //             getlistofMethods(targetMethod, reachableMethods);
    //         }
    //     }
    // }
}
