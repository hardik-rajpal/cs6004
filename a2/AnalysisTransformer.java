import java.util.*;

import soot.*;
public class AnalysisTransformer extends BodyTransformer {
    void print(String s){
        System.out.println(s);
    }
    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        //TODO: handle arrays in PTA
        MyVeryOwnPointsToAnalysis pta = new MyVeryOwnPointsToAnalysis();
        MyVeryOwnEscapeAnalysis ea = new MyVeryOwnEscapeAnalysis();
        TreeMap<Unit, MyVeryOwnPointsToAnalysis.NodePointsToData> pointsToInfo;
        //Step 1: ptg for stack and heap.
        pointsToInfo = pta.getPointsToInfo(body, phaseName, options);
        pta.printPointsToInfo(pointsToInfo);
        //Step 2: escape analysis using said ptg
        // ea.doEscapeAnalysis(body, pointsToInfo);
        
    }
}
