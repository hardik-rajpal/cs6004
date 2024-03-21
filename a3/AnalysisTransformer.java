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
    static TreeMap<String,Set<String>> collectedObjects = new TreeMap<>();
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
    }

    private void processMethod(SootMethod method) {
        if(collectedObjects.containsKey(method.toString())){return;}
        String ans = "";
        ans = method.getDeclaringClass().getName()+":"+method.getName()+" ";
        ans += getAllObjectsGCLines(method);
        results.add(ans);
        return;
    }

    private String getAllObjectsGCLines(SootMethod method) {
        TreeMap<String,String> collectionAfter = new TreeMap<>();
        TreeSet<String> collectedByCallees = new TreeSet<>();
        Body body = method.getActiveBody();
        UnitPatchingChain units = body.getUnits();
        UnitGraph cfg = new BriefUnitGraph(body);
        LiveLocals liveLocals = new SimpleLiveLocals(cfg);
        PTA pta = new PTA();
        TreeMap<String,String> paramMap = new TreeMap<>();
        PTA.CallerInfo callerInfo = new PTA.CallerInfo(new ArrayList<String>(), new PTA.PointsToGraph(), paramMap);
        callerInfo.userDefinedMethods = userDefinedMethods;
        TreeMap<Unit, PTA.NodePointsToData> pointsToInfo = pta.getPointsToInfo(body,callerInfo);
        // pta.printPointsToInfo(pointsToInfo);
        //mark all objects as dead.
        //Get line number after which object can be collected.
        List<Unit> tails = cfg.getTails();
        for(Unit unit:units){
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
            if(PTA.isInvokeStmt(unit)){
                InvokeExpr expr = PTA.getInvokeExprFromInvokeUnit(unit);
                SootMethod calleeMethod = expr.getMethod();
                if(userDefinedMethods.contains(calleeMethod)){
                    String calleeMethodKey = calleeMethod.toString();
                    String callsite = Integer.toString(unit.getJavaSourceStartLineNumber());
                    if(!collectedObjects.containsKey(calleeMethodKey)){
                        //termination guarranteed in the absence of recursion only.
                        processMethod(calleeMethod);
                    }
                    for(String object:collectedObjects.get(calleeMethodKey)){
                        String mutatedObject = mutateContext(object,callsite);
                        collectedByCallees.add(mutatedObject);
                    }
                }
            }
            //all objects reachable from parameters are live=> set parameters as live, always.
            List<Local> paramLocals = body.getParameterLocals();
            localsLiveAfterEditable.addAll(paramLocals);

            TreeSet<String> objects =  getObjectsUnusableAfter(localsLiveAfterEditable,info,collectionAfter);
            for(String object:objects){
                //don't collect dummy objects.
                if(!(object.contains("@")||collectedByCallees.contains(object))){
                    collectionAfter.put(object, Integer.toString(unit.getJavaSourceStartLineNumber()));
                }
            }
        }

        String ans = getOutputString(collectionAfter);
        HashSet<String> totalCollection  = new HashSet<>();
        totalCollection.addAll(collectedByCallees);
        totalCollection.addAll(collectionAfter.keySet());
        collectedObjects.put(method.toString(), totalCollection);
        return ans;
    }

    private String mutateContext(String object, String callsite) {
        String[] parts = object.split(Pattern.quote("("));
        //parts = {"Obj_n","c1,c2,c3)"}
        String ans = "";
        char closeBracket = ')';
        if(parts[1].charAt(0)==closeBracket){
            ans = parts[0] + "(" + callsite + parts[1];
        }
        else{
            ans = parts[0] + "("+ callsite+","+parts[1];
        }
        return ans;
    }

    private String getOutputString(TreeMap<String, String> collectionAfter) {
        String ans = "";
        for(String obj:collectionAfter.keySet()){
            String cleanedObject = obj;
            // cleanedObject = cleanedObject.split(Pattern.quote("_"))[1];
            // cleanedObject = cleanedObject.split(Pattern.quote("("))[0];
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
