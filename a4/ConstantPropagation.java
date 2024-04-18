import java.util.ArrayList;
import java.util.HashMap;

import soot.*;
import soot.jimple.internal.JAssignStmt;
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
        public ConstantValue(ConstantPropagation.ConstantValue constantValue) {
            isTop = constantValue.isTop;
            isBot = constantValue.isBot;
            intValue = constantValue.intValue;
        }
        ConstantValue meet(ConstantValue cv2){
            ConstantValue ans = new ConstantValue();
            if(this.isBot || cv2.isBot){
                ans.isTop = false;
                ans.isBot = true;
            }
            else if(this.isTop && cv2.isTop){
                ans.isTop = true;//true by default
            }
            else if(this.isTop||cv2.isTop){
                if(this.isTop){
                    ans = new ConstantValue(cv2.intValue);
                }
                else{
                    ans = new ConstantValue(this.intValue);
                }
            }
            else{
                if(this.intValue == cv2.intValue){
                    ans = new ConstantValue(this.intValue);
                }
                else{
                    ans.isTop = false;
                    ans.isBot = true;
                }
            }
            return ans;
        }
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
    protected void flowThrough(HashMap<Local, ConstantPropagation.ConstantValue> in, Unit unit, HashMap<Local, ConstantPropagation.ConstantValue> out) {
        copy(in,out);
        if(unit instanceof JAssignStmt){
            Value lhs, rhs;
            JAssignStmt stmt = (JAssignStmt) unit;
            lhs = stmt.getLeftOp();
            rhs = stmt.getRightOp();
            
        }
        else{
            //ignoring all other types of statements
        }
    }

    @Override
    protected void copy(HashMap<Local, ConstantPropagation.ConstantValue> src, HashMap<Local, ConstantPropagation.ConstantValue> dst) {
        dst.clear();
        for(Local local :src.keySet()){
            dst.put(local,new ConstantValue(src.get(local)));
        }
    }

    @Override
    protected void merge(HashMap<Local, ConstantPropagation.ConstantValue> in1, HashMap<Local, ConstantPropagation.ConstantValue> in2, HashMap<Local, ConstantPropagation.ConstantValue> out) {
        for(Local local:in1.keySet()){
            ConstantPropagation.ConstantValue cv1 = in1.get(local);
            ConstantPropagation.ConstantValue cv2 = in2.get(local);
            out.put(local, cv2.meet(cv1));
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
