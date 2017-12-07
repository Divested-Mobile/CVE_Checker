import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Patcher {

    private static final String androidWorkspace = "/mnt/Drive-1/Development/Other/Android_ROMs/Build/LineageOS-14.1/";
    private static final String patchesPath = "/mnt/Drive-1/Development/Other/Android_ROMs/Patches/Linux/";
    private static final String patchesPathScript = "\\$cvePatches/";
    private static final String scriptPrefix = "android_kernel_";
    private static final String scriptOutput = "/mnt/Drive-1/Development/Other/Android_ROMs/Scripts/LineageOS-14.1/CVE_Patchers/";


    public static void main(String[] args) {
        if (args.length > 0) {
            for (String kernel : args) {
                checkAndGenerateScript(kernel);
            }
        } else {
            System.out.println("No kernels passed, accepting input from stdin");
            Scanner s = new Scanner(System.in);
            while (s.hasNextLine()) {
                checkAndGenerateScript(s.nextLine());
            }
        }
    }

    private static String getKernelPath(String kernel) {
        return androidWorkspace + "kernel/" + kernel.replaceAll("_", "/");
    }

    private static boolean doesKernelExist(File kernelPath) {
        return kernelPath.exists();
    }

    private static void checkAndGenerateScript(String kernel) {
        String kernelPath = getKernelPath(kernel);
        if (doesKernelExist(new File(kernelPath))) {
            System.out.println("Starting on " + kernel);
            KernelVersion kernelVersion = getKernelVersion(kernelPath);
            boolean prima = new File(kernelPath + "/drivers/staging/prima/").exists();
            boolean qcacld2 = new File(kernelPath + "/drivers/staging/qcacld-2.0/").exists();
            boolean qcacld3 = new File(kernelPath + "/drivers/staging/qcacld-3.0/").exists();

            ArrayList<String> scriptCommands = new ArrayList<>();

            //The top-level directory contains all patchsets
            File[] patchSets = new File(patchesPath).listFiles(File::isDirectory);
            if (patchSets != null && patchSets.length > 0) {
                Arrays.sort(patchSets);

                //Iterate over all patchsets
                for (File patchSet : patchSets) {
                    String patchSetName = patchSet.getName();
                    System.out.println("\tChecking " + patchSetName);

                    //Get all available versions for a patchset
                    File[] patchSetVersions = patchSet.listFiles(File::isDirectory);
                    ArrayList<String> versions = new ArrayList<>();
                    //Check which versions are applicable
                    for (File patchSetVersion : patchSetVersions) {
                        String patchVersion = patchSetVersion.getName();
                        if (isVersionInRange(kernelVersion, patchVersion)) {
                            versions.add(patchVersion);
                        }
                        if ((prima && patchVersion.equals("prima")) || (qcacld2 && patchVersion.equals("qcacld-2.0")) || (qcacld3 && patchVersion.equals("qcacld-3.0"))) {
                            versions.add(patchVersion);
                        }
                    }

                    boolean depends = new File(patchSet.toString() + "/depends").exists();

                    //Iterate over all applicable versions
                    for (String version : versions) {
                        File[] patches = new File(patchSet.getAbsolutePath() + "/" + version + "/").listFiles(File::isFile);
                        if (patches != null && patches.length > 0) {
                            Arrays.sort(patches);

                            //Check the patches
                            if (depends) {
                                ArrayList<String> commands = doesPatchSetApply(kernelPath, patches, true);
                                if (commands != null) {
                                    scriptCommands.addAll(commands);
                                }
                            } else {
                                for (File patch : patches) {
                                    if (isValidPatchName(patch.getName())) {
                                        String command = doesPatchApply(kernelPath, patch.getAbsolutePath(), true, "");
                                        if (command != null) {
                                            scriptCommands.add(command);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                System.out.println("\tNo patches available");
            }

            System.out.println("\tAttempted to check all patches");
            System.out.println("\tAble to apply " + scriptCommands.size() + " patch(es) against " + kernel);
            try {
                String script = scriptOutput + scriptPrefix + kernel + ".sh";
                PrintWriter out = new PrintWriter(script, "UTF-8");
                out.println("#!/bin/bash");
                out.println("cd $base\"kernel/" + kernel.replaceAll("_", "/") + "\"");
                for (String command : scriptCommands) {
                    out.println(command);
                }
                out.println("cd $base");
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Invalid kernel: " + kernel);
        }
    }

    private static boolean isValidPatchName(String patch) {
        return !patch.contains(".base64") && !patch.contains(".disabled") && !patch.contains(".dupe") && !patch.contains(".sh");
    }

    private static String logPretty(String string, String kernelPath) {
        string = string.replaceAll(kernelPath, "\\$kernelPath");
        string = string.replaceAll(patchesPath, "\\$patchesPath/");
        return string;
    }

    private static String doesPatchApply(String kernelPath, String patch, boolean applyPatch, String alternateRoot) {
        String command = "git -C " + kernelPath + " apply --check " + patch;
        try {
            Process gitCheck = Runtime.getRuntime().exec(command);
            while (gitCheck.isAlive()) {
                //Do nothing
            }
            if (gitCheck.exitValue() == 0) {
                command = command.replaceAll(" --check", "");
                if (alternateRoot.length() > 0) {
                    command += " --directory=\"" + alternateRoot + "\"";
                }
                System.out.println("\t\tPatch can apply successfully: " + logPretty(command, kernelPath));
                if (applyPatch) {
                    Process gitApply = Runtime.getRuntime().exec(command);
                    while (gitApply.isAlive()) {
                        //Do nothing
                    }
                    if (gitApply.exitValue() == 0) {
                        System.out.println("\t\t\tPatch applied successfully: " + logPretty(command, kernelPath));
                    } else {
                        System.out.println("\t\t\tPatched failed to apply after being checked! " + logPretty(command, kernelPath));
                        System.exit(1);
                    }
                }
                return command.replaceAll(" -C " + kernelPath, "").replaceAll(patchesPath, patchesPathScript);
            } else {
                System.out.println("\t\tPatch does not apply successfully: " + logPretty(command, kernelPath));
                if (isWifiPatch(patch)) {
                    System.out.println("\t\t\tThis is a Wi-Fi patch, it might need to be applied directly! Currently unsupported");
                    //TODO: GET THE VERSION
                    //return doesPatchApply(kernelPath, patch, applyPatch, "drivers/staging/" + version);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static ArrayList<String> doesPatchSetApply(String kernelPath, File[] patchset, boolean applyPatches) {
        System.out.println("Checking dependent patchset");
        ArrayList<String> commands = new ArrayList<>();
        for (File patch : patchset) {
            if (isValidPatchName(patch.getName())) {
                String command = doesPatchApply(kernelPath, patch.getAbsolutePath(), applyPatches, "");
                if (command != null) {
                    commands.add(command);
                } else {
                    return null;
                }
            }
        }
        return commands;
    }

    private static KernelVersion getKernelVersion(String kernelPath) {
        String kernelVersion = "";
        try {
            Scanner kernelMakefile = new Scanner(new File(kernelPath + "/Makefile"));
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
            System.out.println("Detected version " + kernelVersion);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new KernelVersion(kernelVersion);
    }

    private static boolean isVersionInRange(KernelVersion kernel, String patch) {
        if (patch.equals("ANY")) {
            return true;
        } else if (kernel.getVersionFull().equals(patch)) {
            return true;
        } else if (patch.startsWith("^")) {
            KernelVersion patchVersion = new KernelVersion(patch.replaceAll("\\^", ""));
            return kernel.isLesserVersion(patchVersion);
        } else if (patch.endsWith("+")) {
            KernelVersion patchVersion = new KernelVersion(patch.replaceAll("\\+", ""));
            return kernel.isGreaterVersion(patchVersion);
        } else if (patch.contains("-^")) {
            String[] patchS = patch.split("-\\^");
            KernelVersion patchVersionLower = new KernelVersion(patchS[0]);
            KernelVersion patchVersionHigher = new KernelVersion(patchS[1]);
            return (kernel.isGreaterVersion(patchVersionLower) && kernel.isLesserVersion(patchVersionHigher));
        }
        return false;
    }

    private static boolean isWifiPatch(String patch) {
        return patch.contains("/prima/") || patch.contains("/qcacld-");
    }

}
