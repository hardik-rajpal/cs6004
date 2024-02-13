import java.util.*;
import soot.*;
import soot.jimple.AnyNewExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JNewExpr;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;

public class AnalysisTransformer extends BodyTransformer {
    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        // Construct CFG for the current method's body
        PatchingChain<Unit> units = body.getUnits();

        // Iterate over each unit of CFG.
        // Shown how to get the line numbers if the unit is a "new" Statement.
        for (Unit u : units) {
            if (u instanceof JAssignStmt) {
                JAssignStmt stmt = (JAssignStmt) u;
                Value rhs = stmt.getRightOp();
                if (rhs instanceof JNewExpr) {
                    try {
                        System.out.println("Unit is " + u + " and the line number is : " + u.getJavaSourceStartLineNumber());
                    } catch (Exception e) {
                        System.out.println("Unit is " + u + " and the line number is : " + -1);
                    }
                }
            }
        }


    }
}
