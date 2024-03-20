import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
            List<SootMethod> methods = classInstance.getMethods();
            for(SootMethod method:methods){
                if(!method.isConstructor()){
                    String ans = processMethod(method);
                    results.add(ans);
                }
            }
        }
    }

    private String processMethod(SootMethod method) {
        String ans = "";
        ans = method.getDeclaringClass().getName()+":"+method.getName()+" ";
        ans += getAllObjectsGCLines(method);
        return ans;
    }

    private String getAllObjectsGCLines(SootMethod method) {
        TreeMap<String,String> collectionAfter = new TreeMap<>();
        Body body = method.getActiveBody();
        UnitPatchingChain units = body.getUnits();
        UnitGraph cfg = new BriefUnitGraph(body);
        LiveLocals liveLocals = new SimpleLiveLocals(cfg);
        PTA pta = new PTA();
        TreeMap<String,String> paramMap = new TreeMap<>();
        PTA.CallerInfo callerInfo = new PTA.CallerInfo(new ArrayList<String>(), new PTA.PointsToGraph(), paramMap);
        TreeMap<Unit, PTA.NodePointsToData> pointsToInfo = pta.getPointsToInfo(body,callerInfo);
        pta.printPointsToInfo(pointsToInfo);
        //mark all objects as dead.
        //Get line number after which object can be collected.
        List<Unit> tails = cfg.getTails();
        for(Unit unit:units){
            // HashSet<Local> localsLiveBefore = new HashSet<>(liveLocals.getLiveLocalsBefore(unit));
            PTA.NodePointsToData info = pointsToInfo.get(unit);
            List<Local> localsLiveAfter = liveLocals.getLiveLocalsAfter(unit);
            HashSet<Local> localsLiveAfterEditable = new HashSet<>(localsLiveAfter);
            //add returned object to live vars after artificially:
            if(tails.contains(unit) && (unit instanceof JReturnStmt)){
                JReturnStmt stmt = (JReturnStmt)unit;
                Value returnValue = stmt.getOp();
                if(returnValue instanceof Local){
                    Local local = (Local)returnValue;
                    localsLiveAfterEditable.add(local);
                }
            }
            //all objects reachable from parameters are live=> set parameters as live, always.
            List<Local> paramLocals = body.getParameterLocals();
            localsLiveAfterEditable.addAll(paramLocals);

            // System.out.println(unit.toString());
            TreeSet<String> objects =  getObjectsUnusableAfter(localsLiveAfterEditable,info,collectionAfter);
            for(String object:objects){
                //don't collect dummy objects.
                if(!object.contains("@")){
                    collectionAfter.put(object, Integer.toString(unit.getJavaSourceStartLineNumber()));
                }
            }
        }

        String ans = getOutputString(collectionAfter);
        // System.exit(0);
        return ans;
    }

    private String getOutputString(TreeMap<String, String> collectionAfter) {
        String ans = "";
        //TODO: modify to clean object up.
        for(String obj:collectionAfter.keySet()){
            ans += obj+":"+collectionAfter.get(obj)+" ";
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

    private TreeSet<String> getObjectsUnusableAfter(HashSet<Local> localsLiveAfter,
            PTA.NodePointsToData info, TreeMap<String, String> collectionAfter) {
        TreeSet<String> allObjects = getAllObjects(info.in);
        allObjects.addAll(getAllObjects(info.out));
        TreeSet<String> liveObjectsAfter = getReachableObjects(info.out,getLocalNames(localsLiveAfter));
        
        //remove live objects
        allObjects.removeAll(liveObjectsAfter);
        //remove collectedObjects
        for(String collectedObject:collectionAfter.keySet()){
            allObjects.remove(collectedObject);
        }
        return allObjects;
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
