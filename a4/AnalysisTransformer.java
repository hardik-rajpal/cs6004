import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import soot.ArrayType;
import soot.Body;
import soot.BodyTransformer;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JDynamicInvokeExpr;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.util.Chain;
public class AnalysisTransformer extends SceneTransformer{
    static CallGraph cg;
    static HashSet<SootMethod> userDefinedMethods = new HashSet<>();

    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {
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
        methodTransform(mainMethod.getActiveBody());
        
    }

    public static Method convertSootToJavaMethod(SootMethod sootMethod) throws ClassNotFoundException, NoSuchMethodException {
        // Get the declaring class of the SootMethod
        String className = sootMethod.getDeclaringClass().getName();
        Class<?> declaringClass = Class.forName(className);

        // Get the method name
        String methodName = sootMethod.getName();
        if(methodName.contains(".")){
            String[] parts = methodName.split(Pattern.quote("."));
            methodName = parts[parts.length-1];
        }
        // Get the parameter types of the method
        Class<?>[] parameterTypes = new Class<?>[sootMethod.getParameterCount()];
        for (int i = 0; i < sootMethod.getParameterCount(); i++) {
            parameterTypes[i] = getClassForType(sootMethod.getParameterType(i));
        }

        // Find and return the corresponding Method using reflection
        // for(Method method: declaringClass.getDeclaredMethods()){
        //     System.out.print(method.getName()+": ");
        //     for(Class type:method.getParameterTypes()){
        //         System.out.print(type.toString()+",");
        //     }
        //     System.out.print("\n");
        // }
        return declaringClass.getDeclaredMethod(methodName, parameterTypes);
    }
    public static Class<?> getClassForType(Type sootType) throws ClassNotFoundException {
        if (sootType instanceof RefType) {
            RefType refType = (RefType) sootType;
            String className = refType.getClassName();
            return Class.forName(className);
        } else if (sootType instanceof PrimType) {
            PrimType primType = (PrimType) sootType;
            switch (primType.toString()) {
                case "int":
                    return int.class;
                case "byte":
                    return byte.class;
                case "short":
                    return short.class;
                case "long":
                    return long.class;
                case "float":
                    return float.class;
                case "double":
                    return double.class;
                case "boolean":
                    return boolean.class;
                case "char":
                    return char.class;
                default:
                    throw new IllegalArgumentException("Unsupported primitive type: " + primType.toString());
            }
        } else if (sootType instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) sootType;
            Class<?> elementType = getClassForType(arrayType.baseType);
            return Array.newInstance(elementType, 0).getClass();
        } else {
            throw new IllegalArgumentException("Unsupported type: " + sootType.getClass().getName());
        }
    }
    public static boolean isInvokeExpression(Value expression) {
        return expression instanceof JInterfaceInvokeExpr || expression instanceof JVirtualInvokeExpr
                || expression instanceof JStaticInvokeExpr || expression instanceof JDynamicInvokeExpr
                || expression instanceof JSpecialInvokeExpr;
    }
    public static class SootMethodComparator implements Comparator<SootMethod>{

        @Override
        public int compare(SootMethod o1, SootMethod o2) {
            return o1.toString().compareTo(o2.toString());
        }
        
    }
    public static boolean isInvokeStmt(Unit u) {
        boolean ans = false;
        if (u instanceof JAssignStmt) {
            Value rhs = ((JAssignStmt) u).getRightOp();
            if (isInvokeExpression(rhs)) {
                ans = true;
            }
        } else if (u instanceof JInvokeStmt) {
            JInvokeStmt stmt = (JInvokeStmt) (u);
            InvokeExpr expr = stmt.getInvokeExpr();
            if (isInvokeExpression(expr)) {
                ans = true;
            }
        }
        return ans;
    }
    public static TreeSet<SootMethod> getSootMethodsFromInvokeUnit(Unit u, CallGraph cg) {
        TreeSet<SootMethod> ans = new TreeSet<>(new SootMethodComparator());
        for (Iterator<Edge> iter = cg.edgesOutOf(u); iter.hasNext();) {
            Edge edge = iter.next();
            SootMethod tgtMethod = edge.tgt();
            ans.add(tgtMethod);
        }
        return ans;
    }
    
    protected void methodTransform(Body body) {
        BriefUnitGraph cfg = new BriefUnitGraph(body);
        for(Unit u:body.getUnits()){
            if(isInvokeStmt(u)){
                CallGraph cg = Scene.v().getCallGraph();
                TreeSet<SootMethod> methods = getSootMethodsFromInvokeUnit(u, cg);
                if(methods.size()==1){
                    SootMethod m = methods.first();
                    // if(m is pure)
                    try{
                        Method javaMethod = convertSootToJavaMethod(m);
                        if(javaMethod.getName().startsWith("foo")){
                            Object result = javaMethod.invoke(null,1,2);
                            print(result.toString());
                        }
                    }
                    catch (Exception e){
                        System.out.println("Didn't work: ");
                        e.printStackTrace();
                    }
                }
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