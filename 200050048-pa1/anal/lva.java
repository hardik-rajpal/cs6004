import java.util.HashMap;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.options.Options;
import soot.toolkits.graph.DominatorsFinder;
import soot.toolkits.graph.MHGDominatorsFinder;
import soot.toolkits.graph.UnitGraph;
public class lva extends ForwardFlowAnalysis<Unit,FlowSet<Value>>{
    private static final FlowSet<Value> phi = new ArraySparseSet<Value>();
    private final Map<Unit,FlowSet<Value>> gen;
    lva(UnitGraph graph){
        super(graph);
        this.gen = new HashMap<Unit,List<Value>>(graph.size()*2+1);
    }
}