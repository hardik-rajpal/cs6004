import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIfStmt;
import soot.toolkits.graph.BriefUnitGraph;

public class AnalysisTransformer extends BodyTransformer{

    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        PackManager.v().getPack("jop").apply(body);
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