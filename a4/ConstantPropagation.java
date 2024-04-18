import java.util.ArrayList;
import java.util.HashMap;

import soot.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class ConstantPropagation extends ForwardFlowAnalysis<Unit,HashMap<Local,ConstantPropagation.ConstantValue>> {
    public static class ConstantValue{
        boolean isTop = true;
        boolean isBot = false;
        // Class<?> valueType = null;
        // String strValue;
        int intValue;
        ConstantValue(int v){
            intValue = v;
            isTop = false;
            // valueType=int.class;
        }
        ConstantValue(){}
        // ConstantValue(String v){
        //     strValue = v;
        //     isTop = false;
        //     valueType = String.class;
        // }
    }
    ArrayList<Local> locals;
    ConstantPropagation(BriefUnitGraph g, ArrayList<Local> _locals){
        super(g);
        this.doAnalysis();
        this.locals = _locals;
    }
    @Override
    protected void flowThrough(HashMap<Local, ConstantPropagation.ConstantValue> arg0, Unit arg1, HashMap<Local, ConstantPropagation.ConstantValue> arg2) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void copy(HashMap<Local, ConstantPropagation.ConstantValue> arg0, HashMap<Local, ConstantPropagation.ConstantValue> arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void merge(HashMap<Local, ConstantPropagation.ConstantValue> in1, HashMap<Local, ConstantPropagation.ConstantValue> in2, HashMap<Local, ConstantPropagation.ConstantValue> out) {
        for(Local local:in1.keySet()){
            ConstantPropagation.ConstantValue cv1 = in1.get(local);
            ConstantPropagation.ConstantValue cv2 = in2.get(local);
            
        }
        
    }

    @Override
    protected HashMap<Local, ConstantPropagation.ConstantValue> newInitialFlow() {
        HashMap<Local,ConstantPropagation.ConstantValue> ans = new HashMap<>();
        for(Local local:locals){
            //put in top values.
            ans.put(local, new ConstantPropagation.ConstantValue());
        }
        return ans;
    }


}
