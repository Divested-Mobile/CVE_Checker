/*
 * Copyright (c) 2020 Divested Computing Group
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Scraper {

  public static void main(String[] args) {
    scrape("");
  }

  public static void scrape(String link) {
    if (link.startsWith("https://source.android.com/security/bulletin/")
        || link.startsWith("https://www.qualcomm.com/company/product-security/bulletins/")
        || link.startsWith("https://www.qualcomm.com/product-security/bulletins/")
        || link.startsWith("https://www.codeaurora.org/security-bulletin/")
        || link.endsWith("Qualcomm.html")) {
      scrapeTable(link);
    } else if (link.contains("cip-kernel-sec")) {
      scrapeCIP(link);
    } else {
      System.out.println("Link unsupported");
    }
  }

  public static void scrapeTable(String link) {
    try {
      URL url = new URL(link);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.connect();
      Scanner page = new Scanner(connection.getInputStream());
      String line = "";
      boolean searching = false;
      while (page.hasNextLine()) {
        line = page.nextLine();

        if (line.startsWith("href")) {
          line = "<a " + line;
        }

        if (line.contains("<a") && !line.contains("</a>")) {
          line += "</a>";
        }

        if (line.contains("<table") || line.contains("Vulnerability details")) {
          searching = true;
        }
        if (line.contains("</table>")) {
          searching = false;
        }
        if (searching) {
          if (line.contains("CVE-") && !line.contains(", CVE-")
              && !line.contains("/company/product-security/bulletins/")) {
            line = line.replace("<td>", "").replaceAll("</td>", "").replaceAll("<br/>", "")
                .replaceAll("</p>", "").replaceAll("<td .*>", "").trim();
            if (line.contains("href")) {
              line = line.split(">")[1].split("<")[0];
            }
            System.out.println(line);
          }
          if (line.contains("http://") || line.contains("https://")) {
            if (line.contains("href =http")) {
              line = line.replaceAll("href =http", "href=\"http").replaceAll(">http", "\">http");
            }
            ArrayList<String> patches = getLinksFromLine(line);
            for (String patch : patches) {
              if (patch.contains("platform/cts") || patch.contains("twitter.com")
                  || patch.contains("/company/product-security/bulletins/")) {
                continue;
              }

              if (patch.contains("torvalds") || patch.contains("kernel.org")
                  || patch.contains("/kernel/") || patch.contains("qcacld")
                  || patch.contains("audio-kernel") || patch.contains("patchwork")
                  || patch.contains("lkml.org") || patch.contains("qca-wifi-host-cmn")
                  || patch.contains("redhat.com") || patch.contains("prima")
                  || patch.contains("spinics.net") || patch.contains("kernel/msm-")) {
                continue; // TODO: Make this a toggle
              }

              patch = patch.replaceAll("%2F", "/");
              System.out.println("\tLink - " + getRepo(patch) + patch);
            }
          }
        }
      }
      page.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static ArrayList<String> getLinksFromLine(String line) {
    ArrayList<String> out = new ArrayList<>();
    Pattern link = Pattern.compile("<a[^>]+href=[\\\"']?([\\\"'>]+)[\\\"']?[^>]*>(.+?)<\\/a>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL); // Credit (CC BY-SA 3.0): https://stackoverflow.com/a/5120599
    Matcher matcher = link.matcher(line);
    while (matcher.find()) {
      out.add(matcher.group().split("href=\"")[1].split("\"")[0]);
    }
    return out;
  }

  public static String getRepo(String line) {
    if (line.contains("android.googlesource.com/platform/")) {
      line = line.split(".com/platform/")[1].split("/\\+/")[0];
      return line + " - ";
    }
    if (line.contains("source.codeaurora.org/quic/la/platform") && !line.contains("kernel")) {
      line = line.split("la/platform/")[1].split("/commit/")[0];
      line = line.replaceAll("vendor/qcom-opensource/", "");
      return line + " - ";
    }
    return "";
  }

  public static void scrapeCIP(String path) {
    List<File> issues = Arrays.asList(new File(path + "/issues/").listFiles(File::isFile));
    if (issues != null && issues.size() > 0) {
      Collections.sort(issues, new AlphanumComparator());
      for (File issue : issues) {
        try {
          System.out.println(issue.getName().replaceAll(".yml", ""));
          Scanner cve = new Scanner(issue);
          String line = "";
          boolean searching = false;
          while (cve.hasNextLine()) {
            line = cve.nextLine();

            if (line.contains("fixed-by:")) {
              searching = true;
            }
            if (line.contains("ignore:")) {
              searching = false;
            }
            if (searching && line.contains("never")) {
              continue;
            }
            if (searching) {
              while (line.endsWith(",")) {
                line += cve.nextLine().replaceAll("    ", " ");
              }
              if (line.contains("mainline:")) {
                final String commit = line.split("\\[")[1].split("\\]")[0];
                if (commit.contains(", ")) {
                  for (String commitS : commit.split(", ")) {
                    System.out.println("\tLink - " + Common.URL_LINUX_MAINLINE + commitS);
                  }
                } else {
                  System.out.println("\tLink - " + Common.URL_LINUX_MAINLINE + commit);
                }
              }
              if (line.contains("stable/")) {
                final String version = line.split("stable/")[1].split(":")[0];
                final String commit = line.split("\\[")[1].split("\\]")[0];
                if (commit.contains(", ")) {
                  for (String commitS : commit.split(", ")) {
                    System.out.println("\tLink - " + version + " - " + Common.URL_LINUX_STABLE + commitS);
                  }
                } else {
                  System.out.println("\tLink - " + version + " - " + Common.URL_LINUX_STABLE + commit);
                }
              }
              if (line.contains("cip/4.4") && !line.contains("4.4-st") && !line.contains("4.4-rt")) {
                final String version = line.split("cip/")[1].split(":")[0];
                final String commit = line.split("\\[")[1].split("\\]")[0];
                if (commit.contains(", ")) {
                  for (String commitS : commit.split(", ")) {
                    System.out.println("\tLink - " + version + " - " + Common.URL_LINUX_CIP + commitS);
                  }
                } else {
                  System.out.println("\tLink - " + version + " - " + Common.URL_LINUX_CIP + commit);
                }
              }
              if (line.contains("aosp/")) {
                final String version = line.split("aosp/")[1].split(":")[0];
                final String commit = line.split("\\[")[1].split("\\]")[0];
                if (commit.contains(", ")) {
                  for (String commitS : commit.split(", ")) {
                    System.out.println("\tLink - " + version + " - " + Common.URL_AOSP_STABLE + commitS);
                  }
                } else {
                  System.out.println("\tLink - " + version + " - " + Common.URL_AOSP_STABLE + commit);
                }
              }
            }
          }
          cve.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

}
