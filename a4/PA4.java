import soot.*;
public class PA4 {
    public static void main(String[] args) {
        String classPath = "./testcases"; 	// change to appropriate path to the test class
        //Set up arguments for Soot
        String[] sootArgs = {
            "-cp", classPath, "-pp", // sets the class path for Soot
            "-keep-line-number", // preserves line numbers in input Java files  
            "-f","c","-p","jop","enabled",
            "-p","jop.cpf",
            "on","classNamePlaceholder",
        };

        // Create transformer for analysis
        // AnalysisTransformer analysisTransformer = new AnalysisTransformer();

        // Add transformer to appropriate pack in PackManager; PackManager will run all packs when soot.Main.main is called
        // PackManager.v().getPack("jtp").add(new Transform("jtp.dfa", analysisTransformer));
        // Call Soot's main method with arguments
        int l = sootArgs.length-1;
        for(int i=0;i<5;i++){
            
            sootArgs[l] = "T"+Integer.toString(i+1);
            processTestcase(i,sootArgs);
        }

    }

    private static void processTestcase(int i, String[] sootArgs) {
        soot.Main.main(sootArgs);
        soot.G.v().reset();
    }
}
