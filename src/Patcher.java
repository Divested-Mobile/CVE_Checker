/*
Copyright (c) 2017-2018 Divested Computing, Inc.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Patcher {

    private static final int REPO_TYPE_KERNEL = 0;
    private static final int REPO_TYPE_ANDROID = 1;

    private static String androidWorkspace = "";
    private static String patchesPathRoot = "";
    private static String patchesPathLinux = "";
    private static String patchesPathAndroid = "";
    private static final String patchesPathScriptLinux = "\\$cvePatchesLinux/";
    private static final String patchesPathScriptAndroid = "\\$cvePatchesAndroid/";
    private static final String scriptPrefix = "android_";
    private static String scriptOutput = "";


    public static void patch(String[] args) {
        if(args.length >= 4) {
            androidWorkspace = args[1];
            patchesPathRoot = args[2];
            scriptOutput = args[3];
            patchesPathLinux = patchesPathRoot + "Linux/";
            patchesPathAndroid = patchesPathRoot + "Android/";
        } else {
            System.out.println("Not enough args");
        }
        if (args.length > 4) {
            int c = 0;
            for (String repo : args) {
                if(c < 4) {
                    c++;
                    continue;
                }
                checkAndGenerateScript(repo, null);
            }
        } else if(args.length == 4) {
            System.out.println("No repos passed, accepting input from stdin");
            Scanner s = new Scanner(System.in);
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.length() > 0) {
                    checkAndGenerateScript(line, null);
                }
            }
        }
    }

    private static int getRepoType(String repoPath) {
        if(repoPath.contains("kernel")) {
            return REPO_TYPE_KERNEL;
        } else {
            return REPO_TYPE_ANDROID;
        }
    }

    private static String getRepoPath(String repo) {
        return androidWorkspace + repo.replaceAll("_", "/");
    }

    private static boolean doesRepoExist(File repoPath) {
        return repoPath.exists();
    }

    private static int wifiVersionSupported = -1;

    private static void checkAndGenerateScript(String repo, ArrayList<String> scriptCommands) {
        String repoPath = getRepoPath(repo);
        if (doesRepoExist(new File(repoPath))) {
            System.out.println("Starting on " + repo);
            boolean firstRun = true;
            int firstPass = 0;
            if(scriptCommands == null) {
                scriptCommands = new ArrayList<>();
            } else {
                firstRun = false;
                firstPass = scriptCommands.size();
            }

            int repoType = getRepoType(repoPath);
            Version repoVersion = null;
            String patchesPath = "";
            String patchesPathScript = "";
            boolean ignoreMajor = false;
            if(repoType == REPO_TYPE_KERNEL) {
                repoVersion = getKernelVersion(repoPath);
                patchesPath = patchesPathLinux;
                patchesPathScript = patchesPathScriptLinux;
                ignoreMajor = false;
            } else if(repoType == REPO_TYPE_ANDROID) {
                repoVersion = getAndroidVersion(androidWorkspace);
                patchesPath = patchesPathAndroid;
                patchesPathScript = patchesPathScriptAndroid;
                ignoreMajor = true;
            }

            if(new File(repoPath + "/drivers/staging/prima/").exists()) {
                wifiVersionSupported = 1;
            }
            if(new File(repoPath + "/drivers/staging/qcacld-2.0/").exists()) {
                wifiVersionSupported = 2;
            }
            if(new File(repoPath + "/drivers/staging/qcacld-3.0/").exists()) {
                wifiVersionSupported = 3;
            }

            //The top-level directory contains all patchsets
            File[] patchSets = new File(patchesPath).listFiles(File::isDirectory);
            if (patchSets != null && patchSets.length > 0) {
                Arrays.sort(patchSets);

                //Iterate over all patchsets
                for (File patchSet : patchSets) {
                    String patchSetName = patchSet.getName();
                    System.out.println("\tChecking " + patchSetName);
                    if(!firstRun && patchSetName.equals("0001-LinuxIncrementals")) {
                        System.out.println("\t\tThis is a second pass, skipping Linux incrementals");
                        continue;
                    }

                    //Get all available versions for a patchset
                    File[] patchSetVersions = patchSet.listFiles(File::isDirectory);
                    ArrayList<String> versions = new ArrayList<>();
                    //Check which versions are applicable
                    for (File patchSetVersion : patchSetVersions) {
                        String patchVersion = patchSetVersion.getName();
                        if (isVersionInRange(repoVersion, patchVersion, ignoreMajor)) {
                            versions.add(patchVersion);
                        }
                        if(repoType == REPO_TYPE_KERNEL) {
                            if ((wifiVersionSupported == 1 && patchVersion.equals("prima")) || (wifiVersionSupported == 2 && patchVersion.equals("qcacld-2.0")) || (wifiVersionSupported == 3 && patchVersion.equals("qcacld-3.0"))) {
                                versions.add(patchVersion);
                            }
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
                                ArrayList<String> commands = doesPatchSetApply(repoPath, patches, true, patchesPath, patchesPathScript);
                                if (commands != null) {
                                    scriptCommands.addAll(commands);
                                }
                            } else {
                                for (File patch : patches) {
                                    if (isValidPatchName(patch.getName())) {
                                        String command = doesPatchApply(repoPath, patch.getAbsolutePath(), true, "", patchesPath, patchesPathScript);
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

            if(scriptCommands.size() > 0) {
                if(firstRun) {
                    System.out.println("\tPerforming second pass to check for unmarked dependents");
                    checkAndGenerateScript(repo, scriptCommands);
                } else {
                    System.out.println("\tAttempted to check all patches against " + repo);
                    System.out.println("\tApplied " + scriptCommands.size() + " patch(es) - 1st Pass: " + firstPass + ", 2nd Pass: " + (scriptCommands.size() - firstPass));
                    writeScript(repo, scriptCommands);
                }
            }
        } else {
            System.out.println("Invalid repo: " + repo);
        }
        wifiVersionSupported = -1;
    }

    private static void writeScript(String repo, ArrayList<String> scriptCommands) {
        try {
            String script = scriptOutput + scriptPrefix + repo + ".sh";
            PrintWriter out = new PrintWriter(script, "UTF-8");
            out.println("#!/bin/bash");
            out.println("cd $base\"" + repo.replaceAll("_", "/") + "\"");
            for (String command : scriptCommands) {
                out.println(command);
            }
            out.println("editKernelLocalversion \"-dos.p" + scriptCommands.size() + "\"");
            out.println("cd $base");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isValidPatchName(String patch) {
        return !patch.contains(".base64") && !patch.contains(".disabled") && !patch.contains(".dupe") && !patch.contains(".sh");
    }

    private static String logPretty(String string, String repoPath) {
        string = string.replaceAll(repoPath, "\\$repoPath");
        string = string.replaceAll(patchesPathRoot, "\\$patchesPathRoot/");
        return string;
    }

    private static int runCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            while (process.isAlive()) {
                //Do nothing
            }
            return process.exitValue();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static String doesPatchApply(String repoPath, String patch, boolean applyPatch, String alternateRoot, String patchesPath, String patchesPathScript) {
        String command = "git -C " + repoPath + " apply --check " + patch;
        if (alternateRoot.length() > 0) {
            command += " --directory=\"" + alternateRoot + "\"";
        }
        try {
            if (runCommand(command) == 0) {
                command = command.replaceAll(" --check", "");
                System.out.println("\t\tPatch can apply successfully: " + logPretty(command, repoPath));
                if (applyPatch) {
                    if (runCommand(command) == 0) {
                        System.out.println("\t\t\tPatch applied successfully: " + logPretty(command, repoPath));
                    } else {
                        System.out.println("\t\t\tPatched failed to apply after being checked! " + logPretty(command, repoPath));
                        System.exit(1);
                    }
                }
                return command.replaceAll(" -C " + repoPath, "").replaceAll(patchesPath, patchesPathScript);
            } else {
                System.out.println("\t\tPatch does not apply successfully: " + logPretty(command, repoPath));
                if (isWifiPatch(patch) && alternateRoot.equals("")) {
                    System.out.println("\t\t\tThis is a Wi-Fi patch, attempting to apply directly!");
                    String altRoot = "drivers/staging/" + getWifiVersionString();
                    return doesPatchApply(repoPath, patch, applyPatch, altRoot, patchesPath, patchesPathScript);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static ArrayList<String> doesPatchSetApply(String repoPath, File[] patchset, boolean applyPatches, String patchesPath, String patchesPathScript) {
        System.out.println("\t\tChecking dependent patchset");
        ArrayList<String> commands = new ArrayList<>();
        for (File patch : patchset) {
            if (isValidPatchName(patch.getName())) {
                String command = doesPatchApply(repoPath, patch.getAbsolutePath(), applyPatches, "", patchesPath, patchesPathScript);
                if (command != null) {
                    commands.add(command);
                } else {
                    return null;
                }
            }
        }
        return commands;
    }

    private static Version getKernelVersion(String kernelPath) {
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
            System.out.println("Detected kernel version " + kernelVersion);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Version(kernelVersion);
    }

    private static Version getAndroidVersion(String androidPath) {
        String androidVersion = "";
        try {
            Scanner repoManifest = new Scanner(new File(androidPath + "/.repo/manifests/default.xml"));
            while(repoManifest.hasNextLine()) {
                String line = repoManifest.nextLine();
                if(line.contains("revision") && line.contains("android")) { //revision="refs/tags/android-7.1.2_r29"
                    String versionTmp = line.trim().split("\"")[1]; //refs/tags/android-7.1.2_r29
                    versionTmp = versionTmp.replaceAll("refs/tags/android-", ""); //7.1.2_r29
                    versionTmp = versionTmp.split("_")[0]; //7.1.2
                    androidVersion = versionTmp;
                    break;
                }
            }
            System.out.println("Detected Android version " + androidVersion);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return new Version(androidVersion);
    }

    private static boolean isVersionInRange(Version repo, String patch, boolean ignoreMajor) {
        if (patch.equals("ANY")) {
            return true;
        } else if (repo.getVersionFull().equals(patch)) {
            return true;
        } else if (patch.startsWith("^")) {
            Version patchVersion = new Version(patch.replaceAll("\\^", ""));
            return repo.isLesserVersion(patchVersion, ignoreMajor);
        } else if (patch.endsWith("+")) {
            Version patchVersion = new Version(patch.replaceAll("\\+", ""));
            return repo.isGreaterVersion(patchVersion, ignoreMajor);
        } else if (patch.contains("-^")) {
            String[] patchS = patch.split("-\\^");
            Version patchVersionLower = new Version(patchS[0]);
            Version patchVersionHigher = new Version(patchS[1]);
            return (repo.isGreaterVersion(patchVersionLower, ignoreMajor) && repo.isLesserVersion(patchVersionHigher, ignoreMajor));
        }
        return false;
    }

    private static boolean isWifiPatch(String patch) {
        return patch.contains("/prima/") || patch.contains("/qcacld-");
    }

    private static String getWifiVersionString() {
        switch(wifiVersionSupported) {
            case 1:
                return "prima";
            case 2:
                return "qcacld-2.0";
            case 3:
                return "qcacld-3.0";
            default:
                return "UNDEFINED";
        }
    }

}
