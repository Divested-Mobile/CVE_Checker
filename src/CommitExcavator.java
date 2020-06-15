/*
 * Copyright (c) 2018 Divested Computing Group
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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class CommitExcavator {

  public static void main(String[] args) {
    extractCommits(new File("/mnt/Drive-3/Patches/Linux/0Extractor/CVE_List-20180510.log"),
        new File("/mnt/Drive-3/Patches/Linux/0Extractor/android_kernel_common-4.4.log"));
  }

  public static void extractCommits(File cveCommitSubjectsList, File kernelCommitSubjectsList) {
    ArrayList<String> cves = readFileIntoArray(cveCommitSubjectsList);
    ArrayList<String> commits = readFileIntoArray(kernelCommitSubjectsList);
    System.out
        .println("Loaded " + cves.size() + " CVE subjects and " + commits.size() + " commits");
    for (String subject : commits) {
      try {
        String tmpSubject = subject.split(" - ")[1];
        if (cves.contains(tmpSubject)) {
          System.out.println(subject);
        }
      } catch (Exception e) {
        System.out.print("FAILED ON " + subject);
      }
    }
  }

  public static ArrayList<String> readFileIntoArray(File file) {
    ArrayList<String> contents = new ArrayList<String>();
    try {
      Scanner s = new Scanner(file);
      while (s.hasNextLine()) {
        String line = s.nextLine();
        line = line.trim();
        line = line.toLowerCase();
        line = line.replaceAll("Subject: ", "").replaceAll("[PATCH] ", "")
            .replaceAll("BACKPORT: ", "").replaceAll("UPSTREAM: ", "").replaceAll("FROMLIST: ", "")
            .replaceAll("ANDROID: ", "");
        contents.add(line);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return contents;
  }

}
