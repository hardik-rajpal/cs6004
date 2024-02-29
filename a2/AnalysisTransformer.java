import java.util.*;

import soot.*;
public class AnalysisTransformer extends BodyTransformer {
    void print(String s){
        System.out.println(s);
    }
    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        // Construct CFG for the current method's body
        MyVeryOwnPointsToAnalysis pta = new MyVeryOwnPointsToAnalysis();
        TreeMap<Unit, MyVeryOwnPointsToAnalysis.NodePointsToData> pointsToInfo = pta.getPointsToInfo(body, phaseName, options);
        pta.printPointsToInfo(pointsToInfo);
        
    }
}
