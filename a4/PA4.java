import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import soot.*;
public class PA4 {
    public static void main(String[] args) {
        String classPath = "."; 	// change to appropriate path to the test class
        //Set up arguments for Soot
        String tcdir = "./testcases";
        String[] sootArgs = {
            "-cp", classPath, "-pp", // sets the class path for Soot
            "-w","-f","J",//TODO: modify to produce bytecode
            "-keep-line-number", // preserves line numbers in input Java files  
            "-process-dir",tcdir,
        };

        // Create transformer for analysis
        AnalysisTransformer analysisTransformer = new AnalysisTransformer();

        // Add transformer to appropriate pack in PackManager; PackManager will run all packs when soot.Main.main is called
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.pfcp", analysisTransformer));
        // Call Soot's main method with arguments
        //compile:
        runShellCommand("rm "+tcdir+"/*.class");
        runShellCommand("javac "+tcdir+"/*.java");
        //copy testcases to .
        List<Path> copiedFiles = copyFilesFromTo(tcdir,".");
        soot.Main.main(sootArgs);
        deleteFiles(copiedFiles);
        //delete testcases from .

    }

    private static void deleteFiles(List<Path> copiedFiles) {
        for(Path file:copiedFiles){
            try {
                Files.delete(file);
            } catch (IOException e) {
                System.out.println("IO exception while deletion.");
            }
        }
    }

    private static List<Path> copyFilesFromTo(String from,String to) {
        Path sourceDir = Paths.get(from); // Specify the source directory
        Path targetDir = Paths.get(to); // Specify the target directory (current directory)
        List<Path> copiedFiles = new ArrayList<Path>();
        try {
            // Iterate over all files and directories in the source directory
            Files.walk(sourceDir)
                .filter(Files::isRegularFile) // Filter out directories
                .forEach(source -> {
                    try {
                        Path target = targetDir.resolve(sourceDir.relativize(source));
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        // System.out.println("Copied: " + source + " to " + target);
                        copiedFiles.add(target);
                    } catch (IOException e) {
                        System.err.println("Failed to copy " + source + ": " + e);
                    }
                });
        } catch (IOException e) {
            System.err.println("Failed to walk through directory: " + e);
        }
        return copiedFiles;
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
}
