import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.BriefUnitGraph;
import soot.util.Chain;
public class AnalysisTransformer extends SceneTransformer{
    static CallGraph cg;
    static HashSet<SootMethod> userDefinedMethods = new HashSet<>();
    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {
        SootMethod mainMethod = fillUserDefMethodsAndGetMainMethod();
        HashSet<SootMethod> impureMethods = PureMethodClassifier.pureMethodClassification(mainMethod, cg);
        // System.out.println("Impure methods: ");
        // for(SootMethod method:impureMethods){
        //     System.out.println(method.toString()+", ");
        // }
        HashSet<SootMethod> pureUserDefinedMethods = new HashSet<>(userDefinedMethods);
        for(SootMethod method:impureMethods){
            pureUserDefinedMethods.remove(method);
        }
        methodTransform(mainMethod.getActiveBody(),pureUserDefinedMethods);
        
    }

    private SootMethod fillUserDefMethodsAndGetMainMethod() {
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
        return mainMethod;
    }
    public static class SootMethodComparator implements Comparator<SootMethod>{

        @Override
        public int compare(SootMethod o1, SootMethod o2) {
            return o1.toString().compareTo(o2.toString());
        }
        
    }
    
    protected void methodTransform(Body body, HashSet<SootMethod> pureUserDefinedMethods) {
        ArrayList<Local> locals = new ArrayList<Local>(body.getLocals());
        ConstantPropagation cp = new ConstantPropagation(new BriefUnitGraph(body), locals, pureUserDefinedMethods,cg);
        cp.printAnalysis();
        ConstantTransformer.transformProgram(body.getMethod(), cp);
    }
    void printSet(Set<?> set){
        for(Object obj:set){
            print(obj.toString()+", ");
        }
        print("\n");
    }
    private void print(String u) {
        System.out.println(u);
    }
    
}