import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.toolkits.graph.BriefUnitGraph;

public class AnalysisTransformer extends BodyTransformer{

    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        SLVA slva = new SLVA(new BriefUnitGraph(body));
        //Transform body using slva.
        /*
        for unit u in ...
            if unit is assignment
                if rhs is function call continue//side effects.
                if lhs not in livelocals after.
                units.remove(unit)//TEST this statement's result.
        */ 
    }
    
}