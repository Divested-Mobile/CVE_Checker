/*
 * Copyright (c) 2017-2020 Divested Computing Group
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Patcher {

  private static final int MODE_DIRECT = 0;
  private static final int MODE_WORKSPACE = 1;
  private static int MODE_CURRENT = -1;
  private static File workspacePath = null;
  private static final String patchesPathScriptLinux = "\\$DOS_PATCHES_LINUX_CVES/";
  private static final String scriptPrefix = "android_";
  private static boolean looseVersions = false;
  private static boolean looseVersionsExtreme = false;
  private static boolean gitMailbox = false;

  public static void patch(String[] args) {
    if(System.getenv("DOS_PATCHER_LOOSE_VERSIONS") != null) {
      looseVersions = System.getenv("DOS_PATCHER_LOOSE_VERSIONS").equalsIgnoreCase("true");
    }
    if(System.getenv("DOS_PATCHER_LOOSE_VERSIONS_EXTREME") != null) {
      looseVersionsExtreme = System.getenv("DOS_PATCHER_LOOSE_VERSIONS_EXTREME").equalsIgnoreCase("true");
    }
    if(System.getenv("DOS_PATCHER_GIT_AM") != null) {
      gitMailbox = System.getenv("DOS_PATCHER_GIT_AM").equalsIgnoreCase("true");
    }
    if (args.length == 1) {
      System.out.println("Mode options are: direct and workspace");
    } else {
      if (args[1].equals("direct")) {
        if (args.length >= 5) {
          MODE_CURRENT = MODE_DIRECT;
          File patchesPath = new File(ensureLeadingSlash(args[2]));
          File outputDir = new File(ensureLeadingSlash(args[3]));

          int c = 0;
          for (String repo : args) {
            if (c < 4) {
              c++;
              continue;
            }
            File repoPath = new File(ensureLeadingSlash(repo.split(":")[0]));
            String repoName = repo.split(":")[1];
            checkAndGenerateScript(repoPath, repoName, patchesPath, outputDir, null);
          }
        } else {
          System.out
              .println("Invalid args: patch direct $patchesPath $outputDir $repoPath:repoName...");
        }
      } else if (args[1].equals("workspace")) {
        if (args.length >= 6) {
          MODE_CURRENT = MODE_WORKSPACE;
          workspacePath = new File(ensureLeadingSlash(args[2]));
          File patchesPath = new File(ensureLeadingSlash(args[3]));
          File outputDir = new File(ensureLeadingSlash(args[4]));

          int c = 0;
          for (String repo : args) {
            if (c < 5) {
              c++;
              continue;
            }
            String repoName = repo;
            File repoPath = getRepoPath(workspacePath, repoName);
            checkAndGenerateScript(repoPath, repoName, patchesPath, outputDir, null);
          }
        } else {
          System.out.println(
              "Invalid args: patch workspace $workspace $patchesPath $outputDir repoName...");
        }
      }
    }

  }

  private static String ensureLeadingSlash(String dir) {
    if (!dir.endsWith("/")) {
      dir += "/";
    }
    return dir;
  }

  private static File getRepoPath(File workspace, String repoName) {
    return new File(ensureLeadingSlash(
        ensureLeadingSlash(workspace.toString()) + repoName.replaceAll("_", "/")));
  }

  private static boolean doesRepoExist(File repoPath) {
    return repoPath.exists();
  }

  private static int wifiVersionSupported = -1;

  private static void checkAndGenerateScript(File repoPath, String repoName, File patchesPath,
      File outputDir, ArrayList<String> scriptCommands) {
    if (doesRepoExist(repoPath)) {
      System.out.println("Starting on " + repoName);
      boolean firstRun = true;
      int firstPass = 0;
      if (scriptCommands == null) {
        scriptCommands = new ArrayList<>();
      } else {
        firstRun = false;
        firstPass = scriptCommands.size();
      }

      Version repoVersion = Common.getKernelVersion(repoPath);
      String patchesPathScript = patchesPathScriptLinux;
      boolean ignoreMajor = false;
      if (new File(repoPath + "/drivers/staging/prima/").exists()) {
        wifiVersionSupported = 1;
      }
      if (new File(repoPath + "/drivers/staging/qcacld-2.0/").exists()) {
        wifiVersionSupported = 2;
      }
      if (new File(repoPath + "/drivers/staging/qcacld-3.0/").exists()) {
        wifiVersionSupported = 3;
      }
      if (new File(repoPath + "/drivers/staging/qca-wifi-host-cmn/").exists()) {
        wifiVersionSupported = 4;
      }

      // The top-level directory contains all patchsets
      List<File> patchSets = Arrays.asList(patchesPath.listFiles(File::isDirectory));
      if (patchSets != null && patchSets.size() > 0) {
        Collections.sort(patchSets, new AlphanumComparator());

        // Iterate over all patchsets
        for (File patchSet : patchSets) {
          String patchSetName = patchSet.getName();
          System.out.println("\tChecking " + patchSetName);
          if (!firstRun && patchSetName.equals("0001-LinuxIncrementals")) {
            System.out.println("\t\tThis is a second pass, skipping Linux incrementals");
            continue;
          }

          // Get all available versions for a patchset
          File[] patchSetVersions = patchSet.listFiles(File::isDirectory);
          Arrays.sort(patchSetVersions, new AlphanumComparator());
          ArrayList<String> versions = new ArrayList<>();
          // Check which versions are applicable
          boolean directMatchAvailable = false;
          for (File patchSetVersion : patchSetVersions) {
            if(patchSetVersion.getName().startsWith(repoVersion.getVersionFull())) {
              directMatchAvailable = true;
            }
          }

          for (File patchSetVersion : patchSetVersions) {
            String patchVersion = patchSetVersion.getName();
            if (isVersionInRange(repoVersion, patchVersion, ignoreMajor)) {
              versions.add(patchVersion);
            }
            if ((wifiVersionSupported == 1 && patchVersion.equals("prima"))
                || (wifiVersionSupported == 2 && patchVersion.equals("qcacld-2.0"))
                || (wifiVersionSupported == 3 && patchVersion.equals("qcacld-3.0"))
                || (wifiVersionSupported == 4 && patchVersion.equals("qca-wifi-host-cmn"))) {
              versions.add(patchVersion);
            }
            if(!directMatchAvailable && looseVersions) {
              //ugly hack to help 3.x
              //4.4 was maintained well and has all the patches
              //3.18 currently has a ton of patches thanks to maintenance from Google/Linaro up until 2021-10
              //3.4 has many backports from the community
              //3.10 and far more so 3.0 are in not great shape
              if (repoVersion.getVersionFull().startsWith("3.0") && (patchVersion.equals("3.4") || (looseVersionsExtreme && (patchVersion.equals("3.10") || patchVersion.equals("3.18") || patchVersion.equals("4.4"))))) {
                versions.add(patchVersion);
              }
              if (repoVersion.getVersionFull().startsWith("3.4") && (patchVersion.equals("3.10") || (looseVersionsExtreme && (patchVersion.equals("3.18") || patchVersion.equals("4.4"))))) {
                versions.add(patchVersion);
              }
              if (repoVersion.getVersionFull().startsWith("3.10") && (patchVersion.equals("3.18") || (looseVersionsExtreme && (patchVersion.equals("4.4"))))) {
                versions.add(patchVersion);
              }
              if (repoVersion.getVersionFull().startsWith("3.18") && (patchVersion.equals("4.4") || (looseVersionsExtreme && (patchVersion.equals("4.9"))))) {
                versions.add(patchVersion);
              }
              if (repoVersion.getVersionFull().startsWith("4.4") && patchVersion.equals("4.9")) {
                versions.add(patchVersion);
              }
            }
          }

          boolean depends = new File(patchSet.toString() + "/depends").exists();

          // Iterate over all applicable versions
          for (String version : versions) {
            File[] patches =
                new File(patchSet.getAbsolutePath() + "/" + version + "/").listFiles(File::isFile);
            if (patches != null && patches.length > 0) {
              Arrays.sort(patches, new AlphanumComparator());

              // Check the patches
              if (depends) {
                ArrayList<String> commands =
                    doesPatchSetApply(repoPath, patchesPath, patches, true, patchesPathScript);
                if (commands != null) {
                  scriptCommands.addAll(commands);
                }
              } else {
                for (File patch : patches) {
                  if (isValidPatchName(patch.getName())) {
                    String command = doesPatchApply(repoPath, patchesPath, patch.getAbsolutePath(),
                        true, "", patchesPathScript);
                    if (command != null && !scriptCommands.contains(command)) {
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

      if (scriptCommands.size() > 0) {
        if (firstRun) {
          System.out.println("\tPerforming second pass to check for unmarked dependents");
          checkAndGenerateScript(repoPath, repoName, patchesPath, outputDir, scriptCommands);
        } else {
          System.out.println("\tAttempted to check all patches against " + repoName);
          System.out.println("\tApplied " + scriptCommands.size() + " patch(es) - 1st Pass: "
              + firstPass + ", 2nd Pass: " + (scriptCommands.size() - firstPass));
          writeScript(repoName, outputDir, scriptCommands);
        }
      }
    } else {
      System.out.println("Invalid repo: " + repoName);
    }
    wifiVersionSupported = -1;
  }

  private static void writeScript(String repoName, File outputDir,
      ArrayList<String> scriptCommands) {
    try {
      String script = "";
      if (MODE_CURRENT == MODE_WORKSPACE) {
        script = outputDir + "/" + scriptPrefix + repoName + ".sh";
      } else if (MODE_CURRENT == MODE_DIRECT) {
        script = outputDir + "/" + repoName + ".sh";
      }
      PrintWriter out = new PrintWriter(script, "UTF-8");
      out.println("#!/bin/bash");
      if (MODE_CURRENT == MODE_WORKSPACE) {
        out.println("cd \"$DOS_BUILD_BASE\"\"" + repoName.replaceAll("_", "/") + "\"");
      }
      for (String command : scriptCommands) {
        out.println(command);
      }
      if (MODE_CURRENT == MODE_WORKSPACE) {
        out.println("editKernelLocalversion \"-dos.p" + scriptCommands.size() + "\"");
        out.println("cd \"$DOS_BUILD_BASE\"");
      }
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static boolean isValidPatchName(String patch) {
    return patch.endsWith(".patch") || patch.endsWith(".diff");
    // return !patch.contains(".base64") && !patch.contains(".disabled") && !patch.contains(".dupe")
    // && !patch.contains(".sh");
  }

  private static String logPretty(String string, File repoPath, File patchesPath) {
    string = string.replaceAll(repoPath.toString(), "\\$repoPath");
    string = string.replaceAll(patchesPath.toString(), "\\$patchesPathRoot/");
    return string;
  }

  private static String doesPatchApply(File repoPath, File patchesPath, String patch,
      boolean applyPatch, String alternateRoot, String patchesPathScript) {
    String command = "git -C " + repoPath + " apply --check " + patch;
    if (alternateRoot.length() > 0) {
      command += " --directory=" + alternateRoot + "";
    }
    if (patch.contains("0001-LinuxIncrementals")) {
      command += " --exclude=Makefile";
    }
    try {
      if (Common.runCommand(command + " --reverse") != 0 && Common.runCommand(command) == 0) {
        command = command.replaceAll(" --check", "");
         if(gitMailbox && isGitPatch(patch)) {
          command = command.replaceAll(" apply ", " am ");
         }
        System.out.println(
            "\t\tPatch can apply successfully: " + logPretty(command, repoPath, patchesPath));
        if (applyPatch) {
          if (Common.runCommand(command) == 0) {
            System.out.println(
                "\t\t\tPatch applied successfully: " + logPretty(command, repoPath, patchesPath));
          } else {
            System.out.println("\t\t\tPatched failed to apply after being checked! "
                + logPretty(command, repoPath, patchesPath));
            System.exit(1);
          }
        }
        return command.replaceAll(" -C " + repoPath, "")
            .replaceAll(ensureLeadingSlash(patchesPath.toString()), patchesPathScript);
      } else {
        System.out.println(
            "\t\tPatch does not apply successfully: " + logPretty(command, repoPath, patchesPath));
        if (isWifiPatch(patch) && alternateRoot.equals("")) {
          System.out.println("\t\t\tThis is a Wi-Fi patch, attempting to apply directly!");
          String altRoot = "drivers/staging/" + getWifiVersionString();
          return doesPatchApply(repoPath, patchesPath, patch, applyPatch, altRoot,
              patchesPathScript);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static ArrayList<String> doesPatchSetApply(File repoPath, File patchesPath,
      File[] patchset, boolean applyPatches, String patchesPathScript) {
    System.out.println("\t\tChecking dependent patchset");
    ArrayList<String> commands = new ArrayList<>();
    for (File patch : patchset) {
      if (isValidPatchName(patch.getName())) {
        String command = doesPatchApply(repoPath, patchesPath, patch.getAbsolutePath(),
            applyPatches, "", patchesPathScript);
        if (command != null) {
          commands.add(command);
        } else {
          return null;
        }
      }
    }
    return commands;
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
      return (repo.isGreaterVersion(patchVersionLower, ignoreMajor)
          && repo.isLesserVersion(patchVersionHigher, ignoreMajor));
    }
    return false;
  }

  private static boolean isGitPatch(String patch) {
    try {
      Scanner file = new Scanner(new File(patch));
      String firstLine = file.nextLine();
      file.close();
      if (firstLine.contains("Mon Sep 17 00:00:00 2001")) {
        return true;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  private static boolean isWifiPatch(String patch) {
    return patch.contains("/prima/") || patch.contains("/qcacld-") || patch.contains("/qca-wifi-");
  }

  private static String getWifiVersionString() {
    switch (wifiVersionSupported) {
      case 1:
        return "prima";
      case 2:
        return "qcacld-2.0";
      case 3:
        return "qcacld-3.0";
      case 4:
        return "qca-wifi-host-cmn";
      default:
        return "UNDEFINED";
    }
  }

}
