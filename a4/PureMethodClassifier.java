import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class PureMethodClassifier {
    static HashSet<SootMethod> impureMethods = new HashSet<>();
    static HashMap<SootMethod, Integer> processingMethods = new HashMap<>();
    // 0 => processing.
    // 1 => processed.
    static CallGraph cg;
    private static HashSet<SootMethod> userDefinedMethods;

    public static void processMethod(SootMethod method) {
        if (processingMethods.containsKey(method)) {
            return;
        }
        processingMethods.put(method, 0);
        for (Unit unit : method.getActiveBody().getUnits()) {
            if (Utils.isInvokeStmt(unit)) {
                TreeSet<SootMethod> callees = Utils.getSootMethodsFromInvokeUnit(unit, cg);
                if (callees.size() == 0) {
                    System.out.println("No callees for: " + unit.toString());
                    continue;
                } else if (callees.size() > 1) {
                    impureMethods.add(method);
                    break;
                } else {
                    SootMethod callee = callees.first();
                    if (!userDefinedMethods.contains(callee)) {
                        System.out.println(method.toString() + "impure because of: " + unit.toString());
                        impureMethods.add(method);
                        printImpureMethods();
                        break;
                    } else {
                        //user defined callee, process for purity check.
                        processMethod(callee);
                        if (processingMethods.get(callee) == 1) {
                            // callee has been processed.
                            if (impureMethods.contains(callee)) {
                                impureMethods.add(method);
                                break;
                            }
                            // else, it's a pure function call
                        }
                    }
                }
            } else {
                if (!isPureUnit(unit)) {
                    // System.out.println("impure: "+unit.toString());
                    impureMethods.add(method);
                    break;
                }
            }
        }
        processingMethods.put(method, 1);
        if(impureMethods.contains(method)){
            markCallersAsImpure(method);
        }
    }

    private static void markCallersAsImpure(SootMethod method) {
        Iterator<Edge> callerIter = cg.edgesInto(method);
        while (callerIter.hasNext()) {
            Edge caller = callerIter.next();
            SootMethod src = caller.src();
            processingMethods.put(src, 1);
            impureMethods.add(src);
        }
        return;
    }

    private static boolean isPureUnit(Unit unit) {
        // check defboxes.
        List<ValueBox> defBoxes = unit.getDefBoxes();
        for (ValueBox defBox : defBoxes) {
            if (!(defBox.getValue() instanceof Local)) {
                return false;
            }
        }
        return true;
    }

    public static HashSet<SootMethod> pureMethodClassification(Set<SootMethod> mainMethods, CallGraph _cg,
            HashSet<SootMethod> _userDefinedMethods) {
        cg = _cg;
        userDefinedMethods = _userDefinedMethods;
        for (SootMethod mainMethod : mainMethods) {
            processMethod(mainMethod);
        }
        printImpureMethods();
        return impureMethods;

    }

    private static void printImpureMethods() {
        System.out.println("impure methods: ");
        for (SootMethod method : impureMethods) {
            System.out.println(method.toString());
        }
    }
}
