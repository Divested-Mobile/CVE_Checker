import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Patcher {

    private static String prefix = "android_kernel_";
    private static String patches = "/mnt/Drive-1/Development/Other/Android_ROMs/Patches/Linux_CVEs/";
    private static String patchesScript = "\\$cvePatches/";
    private static String base = "/mnt/Drive-1/Development/Other/Android_ROMs/Build/LineageOS-14.1/";
    private static String outputBase = "/mnt/Drive-1/Development/Other/Android_ROMs/Scripts/LineageOS-14.1/CVE_Patchers/";

    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        while (s.hasNextLine()) {
            String kernelName = prefix + s.nextLine();
            String kernelPath = kernelName.replaceAll("android_", "").replaceAll("_", "/");
            String kernel = base + kernelPath;
            if (new File(kernel).exists()) {
                genScript(kernelName, kernelPath, kernel);
            } else {
                System.out.println("Kernel does not exist");
            }
        }
    }

    public static void genScript(String kernelName, String kernelPath, String kernel) {
        String output = outputBase + kernelName + ".sh";
        ArrayList<String> scriptCommands = new ArrayList<String>();
        String kernelVersion = "ERROR";

        try {
            Scanner kernelMakefile = new Scanner(new File(base + kernelPath + "/Makefile"));
            while (kernelMakefile.hasNextLine()) {
                String line = kernelMakefile.nextLine();
                if (line.startsWith("VERSION = ")) {
                    kernelVersion = line.split("= ")[1];
                }
                if (line.startsWith("PATCHLEVEL = ")) {
                    kernelVersion += "." + line.split("= ")[1];
                }
                if (line.startsWith("NAME = ")) {
                    break;
                }
            }
            kernelMakefile.close();
            System.out.println("DETECTED VERSION " + kernelVersion);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        File[] cves = new File(patches).listFiles(File::isDirectory);
        if (cves != null && cves.length > 0) {
            Arrays.sort(cves);
            for (File cve : cves) {
                String cveReal = cve.toString().split("/")[8];
                System.out.println("Checking " + cveReal);
                String[] versions = new String[] {kernelVersion, "ANY"};
                for (String version : versions) {
                    File[] cveSubs = new File(cve.getAbsolutePath() + "/" + version + "/").listFiles(File::isFile);
                    if (cveSubs != null && cveSubs.length > 0) {
                        Arrays.sort(cveSubs);
                        Runtime rt = Runtime.getRuntime();
                        int exitCounter = 0;
                        ArrayList<String> commands = new ArrayList<String>();
                        for (File cveSub : cveSubs) {
                            if (!cveSub.toString().contains(".base64") && !cveSub.toString().contains(".disabled") && !cveSub.toString().contains(".dupe")) {
                                try {
                                    String command = "git -C " + kernel + " apply --check " + cveSub.toString();
                                    commands.add(command.replaceAll(" --check", ""));
                                    System.out.println("\tTesting patch: " + command);
                                    Process git = rt.exec(command);
                                    while (git.isAlive()) {
                                        //Do nothing
                                    }
                                    exitCounter += git.exitValue();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (exitCounter == 0) {
                            System.out.println("\tSuccessfully able to patch " + cveReal);
                            for (String command : commands) {
                                try {
                                    System.out.println("\tApplying patch: " + command);
                                    Process git = rt.exec(command);
                                    while (git.isAlive()) {
                                        //Do nothing
                                    }
                                    if (git.exitValue() != 0) {
                                        System.out.println("Failed: " + command);
                                        System.exit(1);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                String commandScript = command.replaceAll(" -C " + kernel, "").replaceAll(patches, patchesScript);
                                scriptCommands.add(commandScript);
                            }
                        } else {
                            System.out.println("\tPatch(es) do(es) not apply cleanly");
                        }
                    } else {
                        System.out.println("\tNo patches available");
                    }
                }
            }
        } else {
            System.out.println("No CVEs available");
        }
        System.out.println("Attempted to check all patches");
        System.out.println("Able to apply " + scriptCommands.size() + " patch(es)");

        try {
            PrintWriter out = new PrintWriter(output, "UTF-8");
            out.println("#!/bin/bash");
            out.println("cd $base\"" + kernelPath + "\"");
            for (String cmd : scriptCommands) {
                out.println(cmd);
            }
            out.println("cd $base");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
