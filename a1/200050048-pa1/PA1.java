import java.io.IOException;
import java.nio.file.*;
import soot.Main;
import soot.G;
public class PA1{
    public static void callSoot(String args[]){
        soot.Main.main(args);
        soot.G.v().reset();
    }
    public static void deleteClassFiles(String tcdir){
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(tcdir))) {
            for (Path path : directoryStream) {
                if(path.toString().endsWith(".class")){
                    Files.delete(path);
                }
            }
        } catch (IOException e) {
            System.out.println("IOException while deleting class files");
        }
    }
    public static int runShellCommand(String cmd){
        ProcessBuilder processBuilder = new ProcessBuilder(cmd.split("\\s+"));
        try{
            Process process = processBuilder.start();
            return process.waitFor();
        }
        catch (IOException e){
            System.out.println("IO Exception");
        }
        catch (InterruptedException e){
            System.out.println("Don't interrupt me!");
        }
        return -1;
    }
    public static void move(String src, String dst){
        try{
            Files.move(Paths.get(src), Paths.get(dst),StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e){
            System.out.println("IO Exception while moving jimple file");
        }
    }
    public static void createJimple(String tcdir, String className){
        String[] jimpleGenArgs = {
            "-pp","-cp",
            tcdir,
            "-f","J",
            className
        };
        callSoot(jimpleGenArgs);
        String src = "sootOutput/"+className+".jimple";
        String dst = tcdir + "/" + className + ".jimple";
        move(src,dst);
    }
    public static void runLVA(String tcdir, String className){
        String[] lvaArgs = {
            "-pp","-cp",
            tcdir,
            "-print-tags",
            "-f","J",
            "-p","jap.lvtagger","on",
            className
        };
        callSoot(lvaArgs);
        String src = "sootOutput/"+className+".jimple";
        String dst = tcdir + "/lva/" + className + ".jimple";
        move(src,dst);
    }
    public static void runNPA(String tcdir, String className){
        String[] npaArgs = {
            "-pp","-cp",
            tcdir,
            "-print-tags",
            "-f","J",
            "-p","jap.npcolorer","on",
            className
        };
        callSoot(npaArgs);
        String src = "sootOutput/"+className+".jimple";
        String dst = tcdir + "/npa/" + className + ".jimple";
        move(src,dst);
    }
    public static void processTestcase(int i){
        String path = "testcases/tc"+i+"/tc"+i+".java";
        String parts[] = path.split("/");
        String tcdir = parts[0]+"/"+parts[1];
        String className = parts[2].split(".java")[0];
        //compile:
        int retcode = runShellCommand("javac "+path);
        if(retcode!=0){
            System.out.println("Testcase "+i+" compilation returned: "+retcode);
            return;
        }
        createJimple(tcdir, className);
        runLVA(tcdir, className);
        runNPA(tcdir, className);
        //remove class files:
        deleteClassFiles(tcdir);
    }
    public static void main(String args[]){
        for(int i=1;i<=5;i++){
            System.out.println("Processing testcase"+i+"...");
            processTestcase(i);
        }
    }
}