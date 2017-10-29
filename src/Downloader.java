import javax.swing.*;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Scanner;

public class Downloader {

    private static String cveJson = "/home/***REMOVED***/Downloads/cves";
    private static String output = "/mnt/Drive-1/Development/Other/Android_ROMs/Patches/Linux_CVEs-New/";
    private static ArrayList<CVE> cves = new ArrayList<CVE>();

    public static void main(String[] args) {
        //Read in all the CVEs from the JSON file
        //https://cve.lineageos.org/api/v1/cves
        try {
            Scanner cve = new Scanner(new File(cveJson));
            String name = "";
            ArrayList<Link> links = new ArrayList<Link>();
            String curDesc = "";
            String curLink = "";
            while(cve.hasNextLine()) {
                String line = cve.nextLine();
                if(line.contains("cve_name")) {
                    //Start the new CVE
                    name = line.split("\"")[3];
                    System.out.println("Starting " + name);
                }
                if(line.contains("\"desc\"")) {
                    if(curDesc.length() > 0) {
                        links.add(new Link(curLink, curDesc));
                        System.out.println("\tAdded a new link to " + curLink);
                        curDesc = curLink = "";
                    }
                    curDesc = line.split("\"")[3];
                }
                if(line.contains("\"link\"")) {
                    curLink = line.split("\"")[3];
                }
                if(line.contains("\"notes\"")) {//End of element
                    if(curDesc.length() > 0) {
                        links.add(new Link(curLink, curDesc));
                        System.out.println("\tAdded a new link to " + curLink);
                        curDesc = curLink = "";
                    }
                    //Added the last CVE we scraped
                    if(name.length() > 0) {
                        cves.add(new CVE(name, links));
                        System.out.println("\tAdded with " + links.size() + " links");
                        name = "";
                        links = new ArrayList<Link>();
                        curDesc = curLink = "";
                    }
                }
            }
            cve.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Downloading patches...");
        int c = 0;
        for(CVE cve : cves) {
            //Only run if we have patches available
            if(cve.getLinks().size() > 0) {
                //Iterate over all links and download if needed
                int linkC = 0;
                for (Link link : cve.getLinks()) {
                    File outDir = new File(output + cve.getId());
                    outDir.mkdirs();
                    downloadFile(getPatchURL(link), new File(outDir.getAbsolutePath() + "/" + linkC + ".patch"), true);
                    System.out.println("\tDownloaded " + link.getURL());
                    linkC++;
                    c++;
                }
            }
            if(c == 10) {
                break;
            }
        }
    }

    private static String getPatchURL(Link link) {
        if(link.getURL().contains("github.com")) {
            return link.getURL() + ".patch";
        } else if(link.getURL().contains("git.kernel.org") || link.getURL().contains("source.codeaurora.org")) {
            return link.getURL().replaceAll("commit", "patch");
        } else if(link.getURL().contains("android.googlesource.com")) {
            return link.getURL() + "?format=TEXT"; //BASE64 ENCODED
        } else if(link.getURL().contains("review.lineageos.org")) {
            String id = link.getURL().split("/")[3];
            //TODO: Dynamically get revision
            return "https://review.lineageos.org/changes/" + id + "/revisions/1/patch?download"; //BASE64 ENCODED
        }
        return "";
    }

    public static void downloadFile(String url, File out, boolean useCache) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(45000);
            connection.setReadTimeout(45000);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:6.0) Gecko/20100101 Firefox/19.0");
            if (useCache && out.exists()) {
                connection.setIfModifiedSince(out.lastModified());
            }
            connection.connect();
            int res = connection.getResponseCode();
            if (res != 304 && (res == 200 || res == 301 || res == 302)) {
                Files.copy(connection.getInputStream(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class CVE {
        private String id;
        private ArrayList<Link> links;

        public CVE(String id, ArrayList<Link> links) {
            this.id = id;
            this.links = links;
        }

        public ArrayList<Link> getLinks() {
            return links;
        }

        public String getId() {
            return id;
        }
    }

    public static class Link {
        private String url;
        private String desc;

        public Link(String url, String desc) {
            this.url = url;
            this.desc = desc;
        }

        public String getURL() {
            return url;
        }

        public String getDesc() {
            return desc;
        }
    }

}
