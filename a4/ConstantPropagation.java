import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import soot.*;
import soot.JastAddJ.Expr;
import soot.jimple.NullConstant;
import soot.jimple.NumericConstant;
import soot.jimple.StringConstant;
import soot.jimple.internal.AbstractJimpleFloatBinopExpr;
import soot.jimple.internal.JAddExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.scalar.Evaluator;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class ConstantPropagation extends ForwardFlowAnalysis<Unit,HashMap<Local,ConstantPropagation.ConstantValue>> {
    public static class ConstantValue{
        boolean isTop = true;
        boolean isBot = false;
        Value value;
        ConstantValue(Value v){
            value = v;
            isTop = false;
            // valueType=int.class;
        }
        ConstantValue(){}
        public ConstantValue(ConstantPropagation.ConstantValue constantValue) {
            isTop = constantValue.isTop;
            isBot = constantValue.isBot;
            value = constantValue.value;
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
                    ans = new ConstantValue(cv2.value);
                }
                else{
                    ans = new ConstantValue(this.value);
                }
            }
            else{
                if(this.value == cv2.value){
                    ans = new ConstantValue(this.value);
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
        @Override
        public String toString() {
            String ans = "";
            if(isTop){
                ans = "top";
            }
            else if(isBot){
                ans = "bot";
            }
            else{
                ans = value.toString();
            }
            return ans;
        }
    }
    ArrayList<Local> locals;
    HashSet<SootMethod> pureMethods;
    CallGraph cg;
    ConstantPropagation(BriefUnitGraph g, ArrayList<Local> _locals, HashSet<SootMethod> _pureMethods, CallGraph _cg){
        super(g);
        locals = _locals;
        pureMethods = _pureMethods;
        cg = _cg;
        this.doAnalysis();
    }
    boolean valueIsConstant(Value v){
        if(v instanceof NumericConstant || v instanceof StringConstant || v instanceof NullConstant){
            return true;
        }
        return false;
    }

    @Override
    protected void flowThrough(HashMap<Local, ConstantPropagation.ConstantValue> in, Unit unit, HashMap<Local, ConstantPropagation.ConstantValue> out) {
        copy(in,out);
        if(unit instanceof JAssignStmt){
            Value lhs, rhs;
            JAssignStmt stmt = (JAssignStmt) unit;
            lhs = stmt.getLeftOp();
            rhs = stmt.getRightOp();
            if(lhs instanceof Local){
                if(valueIsConstant(rhs)){
                    out.put((Local)lhs, new ConstantValue(rhs));
                }
                else if(rhs instanceof Local){
                    out.put((Local)lhs, new ConstantValue(in.get((Local)rhs)));
                }
                else if(rhs instanceof AbstractJimpleFloatBinopExpr){
                    AbstractJimpleFloatBinopExpr expr = ((AbstractJimpleFloatBinopExpr)rhs);
                    ConstantValue cv1 = new ConstantValue(expr.getOp1());
                    ConstantValue cv2 = new ConstantValue(expr.getOp2());
                    ConstantValue ans = new ConstantValue();
                    if(cv1.value instanceof Local){
                        cv1 = in.get((Local)cv1.value);
                    }
                    if(cv2.value instanceof Local){
                        cv2 = in.get((Local)cv2.value);
                    }
                    if(cv1.isBot || cv2.isBot){
                        ans.isTop = false;
                        ans.isBot = true;
                    }
                    else if(cv1.isTop || cv2.isTop){
                        //remains top
                    }
                    else{
                        //both are non-top, non-bot values.
                        expr = (AbstractJimpleFloatBinopExpr)expr.clone();
                        expr.setOp1(cv1.value);
                        expr.setOp2(cv2.value);
                        ans.isTop = false;
                        ans.value = Evaluator.getConstantValueOf(expr);
                    }
                    out.put((Local)lhs, ans);
                }
                else if(Utils.isInvokeExpression(rhs)){
                    TreeSet<SootMethod> methods = Utils.getSootMethodsFromInvokeUnit(unit, cg);
                    //TODO: merge multiple callees to bot.
                    for(SootMethod method:methods){
                        if(!pureMethods.contains(method)){
                            ConstantValue bot = new ConstantValue();
                            bot.isBot = true;
                            bot.isTop = false;
                            out.put((Local)lhs, bot);
                            break;
                        }
                        //pure method => evaluate if contant.
                        //expr = getInvokeexpr...
                        //if (for each arg in expr, in.get(arg) is constant)
                        //run reflectioncode.
                    }
                }
            }
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
    public void printAnalysis() {
        this.graph.forEach((Unit unit)->{
            synchronized(System.out){
                System.out.println(unit.toString() +"{");
                HashMap<Local, ConstantPropagation.ConstantValue> flow = this.getFlowBefore(unit);
                System.out.println("\tin: {");
                for(Local local:flow.keySet()){
                    System.out.println("\t\t"+local.toString()+"->"+flow.get(local).toString()+",");
                }
                System.out.println("\t},");
                System.out.println("\tout: {");
                flow = this.getFlowAfter(unit);
                for(Local local:flow.keySet()){
                    System.out.println("\t\t"+local.toString()+"->"+flow.get(local).toString()+",");
                }
                System.out.println("\t}\n}");
            }
        });
    }


}
