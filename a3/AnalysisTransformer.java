import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
import soot.jimple.toolkits.callgraph.Edge;
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
        TreeMap<String,Integer> collectionAfter = new TreeMap<>();
        TreeSet<String> collectedByCallees = new TreeSet<>();
        Body body = method.getActiveBody();
        UnitGraph cfg = new BriefUnitGraph(body);
        LiveLocals liveLocals = new SimpleLiveLocals(cfg);
        PTA pta = new PTA();
        TreeMap<String,String> paramMap = new TreeMap<>();
        PTA.CallerInfo callerInfo = new PTA.CallerInfo(new ArrayList<String>(), new PTA.PointsToGraph(), paramMap);
        callerInfo.userDefinedMethods = userDefinedMethods;
        callerInfo.cg = cg;
        pta.runLiveLocalsGC(body,callerInfo,collectionAfter,liveLocals);

        return getOutputString(collectionAfter);
    }
//  TODO: remove heap cloning :(
    private String getOutputString(TreeMap<String, Integer> collectionAfter) {
        String ans = "";
        for(String obj:collectionAfter.keySet()){
            String cleanedObject = obj;
            // cleanedObject = cleanedObject.split(Pattern.quote("_"))[1];
            // cleanedObject = cleanedObject.split(Pattern.quote("("))[0];
            ans += cleanedObject+":"+Integer.toString(collectionAfter.get(obj))+" ";
        }
        return ans;
    }
}
