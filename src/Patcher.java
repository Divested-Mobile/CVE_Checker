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

        KernelVersion kernelVersion = getKernelVersion(kernelPath);
        boolean prima = new File(base + kernelPath + "/drivers/staging/prima/").exists();
        boolean qcacld2 = new File(base + kernelPath + "/drivers/staging/qcacld-2.0/").exists();
        boolean qcacld3 = new File(base + kernelPath + "/drivers/staging/qcacld-3.0/").exists();

        File[] patchSets = new File(patches).listFiles(File::isDirectory);
        if (patchSets != null && patchSets.length > 0) {
            Arrays.sort(patchSets);
            for (File patchSet : patchSets) {
                String patchSetReal = patchSet.toString().split("/")[8];
                System.out.println("Checking " + patchSetReal);
                File[] patchSetVersions =  new File(patchSet.getAbsolutePath()).listFiles(File::isDirectory);
                ArrayList<String> versions = new ArrayList<String>();
                for (File patchSetVersion : patchSetVersions) {
                    String patchVersion = patchSetVersion.getAbsolutePath().split("/")[9];
                    if(isVersionInRange(kernelVersion, patchVersion)) {
                        versions.add(patchVersion);
                    }
                    if((prima && patchVersion.equals("prima")) || (qcacld2 && patchVersion.equals("qcacld-2.0")) || (qcacld3 && patchVersion.equals("qcacld-3.0"))) {
                        versions.add(patchVersion);
                    }
                }
                boolean depends = new File(patchSet.toString() + "/depends").exists();
                if(depends) {
                    System.out.println("\tTHIS IS A DEPENDENT PATCHSET");
                }
                for (String version : versions) {
                    File[] patches = new File(patchSet.getAbsolutePath() + "/" + version + "/").listFiles(File::isFile);
                    if (patches != null && patches.length > 0) {
                        Arrays.sort(patches);
                        int exitCounter = 0;
                        String patchSetFiles = "";
                        ArrayList<String> commands = new ArrayList<String>();
                        for(File patch : patches) {
                            if (!patch.toString().contains(".base64") && !patch.toString().contains(".disabled") && !patch.toString().contains(".dupe") && !patch.toString().contains(".sh")) {
                                if(depends) {
                                    patchSetFiles += " " + patch.toString();
                                } else {
                                    try {
                                        String command = "git -C " + kernel + " apply --check " + patch.toString();
                                        if (isWifiPatch(version)) {
                                            command += " --directory=\"drivers/staging/" + version + "\"";
                                        }
                                        System.out.println("\tTesting patchset: " + command);
                                        Process git = Runtime.getRuntime().exec(command);
                                        while (git.isAlive()) {
                                            //Do nothing
                                        }
                                        if (git.exitValue() == 0) {
                                            commands.add(command.replaceAll(" --check", ""));
                                            System.out.println("\tPatch applies successfully");
                                        } else {
                                            System.out.println("\tPatch does not apply");
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }

                        if(depends && patchSetFiles.length() > 0) {
                            try {
                                String command = "git -C " + kernel + " apply --check " + patchSetFiles;
                                //TODO: FIXME! PASSING MULTIPLE FILES DOESN'T APPLY EACH ONE AFTER THE OTHER BUT ONE AT A TIME SEPARATELY
                                commands.add(command.replaceAll(" --check", ""));
                                if (isWifiPatch(version)) {
                                    command += " --directory=\"drivers/staging/" + version + "\"";
                                }
                                System.out.println("\tTesting patchset: " + command);
                                Process git = Runtime.getRuntime().exec(command);
                                while (git.isAlive()) {
                                    //Do nothing
                                }
                                exitCounter += git.exitValue();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if(exitCounter == 0) {
                            for (String command : commands) {
                                try {
                                    System.out.println("\tApplying patch: " + command);
                                    Process git = Runtime.getRuntime().exec(command);
                                    while (git.isAlive()) {
                                        //Do nothing
                                    }
                                    if (git.exitValue() != 0) {
                                        System.out.println("Potential duplicate patch detected!");
                                        System.out.println("Failed: " + command);
                                        System.exit(1);
                                    } else {
                                        System.out.println("\tSuccessfully able to apply patch");
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                String commandScript = command.replaceAll(" -C " + kernel, "").replaceAll(Patcher.patches, patchesScript);
                                scriptCommands.add(commandScript);
                            }
                        } else {
                                System.out.println("\tUnable to apply patches");
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

    public static boolean isVersionInRange(KernelVersion kernel, String patch) {
        if(patch.equals("ANY")) {
            return true;
        } else if (kernel.getVersionFull().equals(patch)) {
            return true;
        } else if(patch.startsWith("^")) {
            KernelVersion patchVersion = new KernelVersion(patch.replaceAll("\\^", ""));
            return kernel.isLesserVersion(patchVersion);
        } else if(patch.endsWith("+")) {
            KernelVersion patchVersion = new KernelVersion(patch.replaceAll("\\+", ""));
            return kernel.isGreaterVersion(patchVersion);
        } else if(patch.contains("-^")) {
            String[] patchS = patch.split("-\\^");
            KernelVersion patchVersionLower = new KernelVersion(patchS[0]);
            KernelVersion patchVersionHigher = new KernelVersion(patchS[1]);
            return (kernel.isGreaterVersion(patchVersionLower) && kernel.isLesserVersion(patchVersionHigher));
        }
        return false;
    }

    public static KernelVersion getKernelVersion(String path) {
        String kernelVersion = "";
        try {
            Scanner kernelMakefile = new Scanner(new File(base + path + "/Makefile"));
            while (kernelMakefile.hasNextLine()) {
                String line = kernelMakefile.nextLine();
                if (line.startsWith("VERSION = ")) {
                    kernelVersion = line.split("= ")[1];
                }
                if (line.startsWith("PATCHLEVEL = ")) {
                    kernelVersion += "." + line.split("= ")[1];
                }
                if (line.startsWith("SUBLEVEL = ")) {
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
        return new KernelVersion(kernelVersion);
    }

    public static boolean isWifiPatch(String version) {
        return version.equals("prima") || version.startsWith("qcacld");
    }

    public static class KernelVersion {

        private String versionFull = "";
        private int version = 0;
        private int patchLevel = 0;
        private int subLevel = 0;

        public KernelVersion(String version) {
            this.versionFull = version;
            String[] versionSplit = version.split("\\.");
            this.version = Integer.valueOf(versionSplit[0]);
            this.patchLevel = Integer.valueOf(versionSplit[1]);
            if(versionSplit.length == 3) {
                this.subLevel = Integer.valueOf(versionSplit[2]);
            }
        }

        public KernelVersion(int version, int patchLevel) {
            this.versionFull = version + "." + patchLevel;
            this.version = version;
            this.patchLevel = patchLevel;
        }

        public String getVersionFull() {
            return versionFull;
        }

        public int getVersion() {
            return version;
        }

        public int getPatchLevel() {
            return patchLevel;
        }

        public int getSubLevel() {
            return subLevel;
        }

        public boolean isGreaterVersion(KernelVersion comparedTo) {
            if(getVersion() > comparedTo.getVersion()) {
                return true;
            }
            if(getVersion() == comparedTo.getVersion() && getPatchLevel() >= comparedTo.getPatchLevel()) {
                return true;
            }
            return false;
        }

        public boolean isLesserVersion(KernelVersion comparedTo) {
            if(getVersion() < comparedTo.getVersion()) {
                return true;
            }
            if(getVersion() == comparedTo.getVersion() && getPatchLevel() <= comparedTo.getPatchLevel()) {
                return true;
            }
            return false;
        }
    }

}
