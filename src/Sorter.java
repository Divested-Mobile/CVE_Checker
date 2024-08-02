/*
Copyright (c) 2017-2024 Divested Computing Group

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class Sorter {

  private static ArrayList<CVE> cves = new ArrayList<>();

  public static void sort(File manifest) {
    Common.initEnv();
    try {
      Scanner s = new Scanner(manifest);
      String curId = "";
      ArrayList<String> lines = new ArrayList<String>();
      while (s.hasNextLine()) {
        String line = s.nextLine();
        if (!line.startsWith("#") && (line.startsWith("CVE") || line.startsWith("LVT"))) {
          if (curId.length() > 0) {
            // System.out.println("Added a new CVE - " + curId);
            cves.add(new CVE(curId, lines));
            curId = "";
            lines = new ArrayList<String>();
          }
          curId = line;
        } else {
          if(line.contains("=")) {
            String commitID = line.split("=")[1];
            lines.removeIf(previous -> previous.contains(commitID) && (previous.contains("android.googlesource") || previous.contains("cip/linux-cip")));
          }
          lines.add(line);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    Collections.sort(cves, new AlphanumComparator());
    for (CVE cve : cves) {
      if (cve.getLines().size() == 0) {
        continue;
      }
      System.out.println(cve.getId());
      for (String line : cve.getLines()) {
        if(Common.INCLUSIVE_KERNEL_PATH != null && (line.contains("Link - ^") || (line.contains("Link - https") && !manifest.toString().contains("CIP.")))
                && (line.contains(Common.URL_LINUX_MAINLINE) || line.contains(Common.URL_LINUX_STABLE) || line.contains(Common.URL_AOSP_STABLE))) {
          String[] lineSplit = line.split(" - ");
          String commitID = null;
          int offset = 1;
          if (line.contains("Link - ^")) {
            offset = 2;
          }
          if(lineSplit[offset].contains("=")) {
            commitID = lineSplit[offset].split("=")[1];
          }
          if(lineSplit[offset].contains("/+/")) {
            commitID = lineSplit[offset].split("/\\+/")[1];
          }
          if(commitID == null) {
            System.out.println("Unable to extract commit ID");
            System.exit(1);
          }
          Version kernelVersion = Common.getPatchVersion(commitID);
          if(kernelVersion != null) {
            if(line.startsWith("#")) {
              line = "#";
            } else {
              line = "";
            }
            line += "\tLink - ^" + kernelVersion.getVersionFull() + " - " + lineSplit[offset].trim();
          }
        }
        System.out.println(line);
      }
    }
  }

  public static class CVE {
    private String id;
    private ArrayList<String> lines;

    public CVE(String id, ArrayList<String> lines) {
      this.id = id.replaceAll("\u00AD", "-").replaceAll("--", "-");
      this.lines = lines;
    }

    public String getId() {
      return id;
    }

    public ArrayList<String> getLines() {
      return lines;
    }

    @Override
    public String toString() {
      return getId();
    }
  }

}
