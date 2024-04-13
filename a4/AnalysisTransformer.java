import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.BodyTransformer;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIfStmt;
import soot.toolkits.graph.BriefUnitGraph;

public class AnalysisTransformer extends BodyTransformer{

    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        BriefUnitGraph cfg = new BriefUnitGraph(body);
        SLVA slva = new SLVA(cfg);
        //Transform body using slva.
        /*
        for unit u in ...
            if unit is assignment
                if rhs is function call continue//side effects.
                if lhs not in livelocals after.
                units.remove(unit)//TEST this statement's result.
        */
        /*
         if(u) 
        */
        for(Unit u:body.getUnits()){
            if(u instanceof JAssignStmt){

            }
            else if(u instanceof JIfStmt){
                if(cfg.getSuccsOf(u).size()==1){
                    //remove from body.
                }
            }
            synchronized(System.out){
                print(u.toString());
                print("{\n");
                printSet((slva.getLiveLocalsBefore(u)));
                printSet(slva.getLiveLocalsAfter(u));
                print("}\n");
            }
            
        } 
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