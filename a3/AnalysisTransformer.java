import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.UnitPatchingChain;
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
        String ans = "";
        TreeMap<String,String> collectionAfter = new TreeMap<>();
        Body body = method.getActiveBody();
        UnitPatchingChain units = body.getUnits();
        UnitGraph cfg = new BriefUnitGraph(body);
        LiveLocals liveLocals = new SimpleLiveLocals(cfg);
        PTA pta = new PTA();
        TreeMap<String,String> paramMap = new TreeMap<>();
        PTA.CallerInfo callerInfo = new PTA.CallerInfo("", new PTA.PointsToGraph(), paramMap);
        TreeMap<Unit, PTA.NodePointsToData> pointsToInfo = pta.getPointsToInfo(body,callerInfo);
        //mark all objects as dead.
        //Get line number after which object can be collected.
        for(Unit unit:units){
            List<Local> localsLiveBefore = liveLocals.getLiveLocalsBefore(unit);
            PTA.NodePointsToData info = pointsToInfo.get(unit);
            List<Local> localsLiveAfter = liveLocals.getLiveLocalsAfter(unit);
            TreeSet<String> objects =  getObjectsUnusableAfter(localsLiveBefore,localsLiveAfter,info);
            for(String object:objects){
                collectionAfter.put(object, Integer.toString(unit.getJavaSourceStartLineNumber()));
            }
        }
        for(String obj:collectionAfter.keySet()){
            ans += obj+":"+collectionAfter.get(obj)+" ";
        }
        return ans;
    }

    private TreeSet<String> getObjectsUnusableAfter(List<Local> localsLiveBefore, List<Local> localsLiveAfter,
            PTA.NodePointsToData info) {
        TreeSet<String> ans = new TreeSet<>();
        //TODO:
        return ans;
    }

    protected static void processCFG(SootMethod method) {
        if(method.toString().contains("init")  ) { return; }
        Body body = method.getActiveBody();
        // Get the callgraph 
        UnitGraph cfg = new BriefUnitGraph(body);
        // Get live local using Soot's exiting analysis
        LiveLocals liveLocals = new SimpleLiveLocals(cfg);
        // Units for the body
        PatchingChain<Unit> units = body.getUnits();
        System.out.println("\n----- " + body.getMethod().getName() + "-----");
        for (Unit u : units) {
            System.out.println("Unit: " + u);
            List<Local> before = liveLocals.getLiveLocalsBefore(u);
            List<Local> after = liveLocals.getLiveLocalsAfter(u);
            System.out.println("Live locals before: " + before);
            System.out.println("Live locals after: " + after);
            System.out.println();

        }
    }

    // private static void getlistofMethods(SootMethod method, Set<SootMethod> reachableMethods) {
    //     // Avoid revisiting methods
    //     if (reachableMethods.contains(method)) {
    //         return;
    //     }
    //     // Add the method to the reachable set
    //     reachableMethods.add(method);

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
