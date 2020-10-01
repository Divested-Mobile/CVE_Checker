/*
 * Copyright (c) 2017-2018 Divested Computing Group
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

public class Main {

  public static void main(String[] args) {
    if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")
        || args[0].equals("/?")) {
      printHeader();
      printHelp();
    }

    if (args.length > 0) {
      if (args[0].equals("download") && args.length == 2) {
        Downloader.download(new File(args[1]));
      }

      if (args[0].equals("patch") && args.length >= 1) {
        Patcher.patch(args);
      }

      if (args[0].equals("sort") && args.length == 2) {
        Sorter.sort(new File(args[1]));
      }

      if (args[0].equals("linuxIncr") && args.length == 4) {
        LinuxIncrs.generateScript(args[1], args[2], Integer.valueOf(args[3]));
      }

      if (args[0].equals("scraper") && args.length == 2) {
        Scraper.scrape(args[1]);
      }
    }
  }

  private static void printHeader() {
    System.out.println("DivestOS Patch Downloader/Checker");
    System.out.println("Copyright 2017-2020 Divested Computing Group");
    System.out.println("License: GPLv3");
    System.out.println("");
  }

  private static void printHelp() {
    String launchCommand = "java -jar patcher.jar";
    System.out.println("Multiple functions are available");

    System.out.println("\tPrimary");
    System.out.println("\t\tdownload [manifest]");
    System.out.println("\t\tpatch (see examples below)");

    System.out.println("\tSecondary");
    System.out.println("\t\tlinuxIncr [version] [patchLevel] [mostRecentSubLevel]");
    System.out.println("\t\tsort [manifest]");
    System.out.println("\t\tscraper [http link]");
    System.out.println("");

    System.out.println("Examples");
    System.out.println("\tTo download all patches in a manifest to directory of manifest");
    System.out.println(
        "\t\t" + launchCommand + " download /mnt/dos/Patches/Linux/Kernel_CVE_Patch_List.txt");

    System.out.println("\tTo patch a kernel directly");
    System.out.println(
        "\t\t" + launchCommand + " patch direct $patchesPath/ $outputDir/ $repoPath:repoName...");

    System.out.println("\tTo patch a kernel in workspace");
    System.out.println("\t\t" + launchCommand
        + " patch workspace $workspace/ $patchesPath/ $outputDir/ repoName...");

    System.out.println("\tTo generate a Linux kernel incremental patch downloader");
    System.out.println("\t\t" + launchCommand + " linuxIncr 3.x 3.4. 110");

    System.out.println("\tTo sort a manifest");
    System.out
        .println("\t\t" + launchCommand + " sort /mnt/dos/Patches/Linux/Kernel_CVE_Patch_List.txt");

    System.out.println("\tTo scrape CVE patches from an ASB");
    System.out.println("\t\t" + launchCommand + " scraper $ASB_HTTP_LINK");
  }

}
