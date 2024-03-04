import java.util.*;

import soot.*;
public class AnalysisTransformer extends BodyTransformer {
    void print(String s){
        System.out.println(s);
    }
    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        PTA pta = new PTA();
        MyVeryOwnEscapeAnalysis ea = new MyVeryOwnEscapeAnalysis();
        TreeMap<Unit, PTA.NodePointsToData> pointsToInfo;
        //Step 1: ptg for stack and heap.
        pointsToInfo = pta.getPointsToInfo(body, phaseName, options);
        //Step 2: escape analysis using said ptg
        TreeSet<String> fugitives = ea.doEscapeAnalysis(body, pointsToInfo,pta);
        String methodName = body.getMethod().getName();
        String enclosingClass = body.getMethod().getDeclaringClass().getName();
        synchronized(System.out){
            // pta.printPointsToInfo(pointsToInfo);
            // System.out.println("Escaping Info");
            System.out.print(enclosingClass+":"+methodName+" ");
            for(String fugitive:fugitives){
                System.out.print(fugitive.replace("Obj_", "")+" ");
            }
            System.out.print("\n");
        }
    }
}
