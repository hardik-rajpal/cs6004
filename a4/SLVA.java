import java.util.Set;
import java.util.TreeSet;

import soot.Body;
import soot.Local;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;

public class SLVA extends BackwardFlowAnalysis<Unit,FlowSet<Local>> implements StrongLiveVars{
    SLVA(BriefUnitGraph g){
        super(g);
    }

    @Override
    public Set<Local> getLiveLocalsAfter(Unit unit) {
        return new TreeSet<>(getFlowAfter(unit).toList());
    }

    @Override
    public Set<Local> getLiveLocalsBefore(Unit unit) {
        return new TreeSet<>(getFlowBefore(unit).toList());
    }

    @Override
    protected void flowThrough(FlowSet<Local> arg0, Unit arg1, FlowSet<Local> arg2) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void copy(FlowSet<Local> arg0, FlowSet<Local> arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void merge(FlowSet<Local> arg0, FlowSet<Local> arg1, FlowSet<Local> arg2) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected FlowSet<Local> newInitialFlow() {
        // TODO Auto-generated method stub
        return null;
    }

    public void printBody(Body body) {
        // TODO:
    }
}
