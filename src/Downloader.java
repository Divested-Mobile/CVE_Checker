import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Scanner;

public class Downloader {

    private static final String[] linuxVersions = new String[] {"3.0", "3.2", "3.4", "3.8", "3.10", "3.12", "3.16", "3.18", "4.4", "4.5", "4.8"};
    private static String cveJson = "/home/***REMOVED***/Downloads/cves";
    private static String output = "/mnt/Drive-1/Development/Other/Android_ROMs/Patches/Linux_CVEs-New/";
    private static ArrayList<CVE> cves = new ArrayList<CVE>();

    public static void main(String[] args) {
        //Read in all the CVEs from the JSON file
        //https://cve.lineageos.org/api/v1/cves
        try {
            System.out.println("Parsing...");
            Scanner cve = new Scanner(new File(cveJson));
            String name = "";
            ArrayList<Link> links = new ArrayList<Link>();
            String curNotes = "";
            while (cve.hasNextLine()) {
                String line = cve.nextLine();
                String[] lineS = line.split("\"");
                if (line.contains("cve_name") || !cve.hasNextLine()) {
                    if (name.length() > 0) {
                        cves.add(new CVE(name, curNotes, links));
                        System.out.println("\t\tAdded " + links.size() + " links");
                        links = new ArrayList<Link>();
                        name = curNotes = "";
                    }
                    if (cve.hasNextLine()) {
                        name = lineS[3];
                        System.out.println("\t" + name);
                    }
                }
                if (line.contains("\"cve_id\"")) {
                    cve.nextLine();//oid
                    cve.nextLine();//}
                    line = cve.nextLine();
                    String desc = "";
                    String link = "";
                    if (line.contains("\"desc\"")) {
                        desc = line.split("\"")[3];
                    }
                    if (line.contains("\"link\"")) {
                        link = line.split("\"")[3];
                    } else {
                        line = cve.nextLine();
                        if (line.contains("\"link\"")) {
                            link = line.split("\"")[3];
                        }
                    }
                    if (link.length() > 0) {
                        links.add(new Link(link, desc));
                        System.out.println("\t\tAdded a new link to " + link);
                    }
                }
                if (line.contains("\"notes\"")) {
                    //curNotes = lineS[3];
                    //System.out.println("\t\tAdded a new note: " + curNotes);
                }
            }
            cve.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Downloading patches...");
        int c = 0;
        for (CVE cve : cves) {
            System.out.println("\t" + cve.getId());
            //Only run if we have patches available
            if (cve.getLinks().size() > 0) {
                //Iterate over all links and download if needed
                int linkC = 0;
                for (Link link : cve.getLinks()) {
                    String patch = getPatchURL(link);
                    if (!patch.equals("NOT A PATCH")) {
                        File outDir = new File(output + cve.getId() + "/" + getPatchVersion(cve, link));
                        outDir.mkdirs();
                        String base64 = "";
                        if (isBase64Encoded(link)) {
                            base64 = ".base64";
                        }
                        downloadFile(patch, new File(outDir.getAbsolutePath() + "/" + linkC + ".patch" + base64), true);
                        System.out.println("\t\tDownloaded " + link.getURL());
                        linkC++;
                    }
                    c++;//DEBUG
                }
            }
/*            if(c == 30) {//DEBUG
                break;
            }*/
        }
    }

    private static String getPatchURL(Link link) {
        if (link.getURL().contains("github.com")) {
            return link.getURL() + ".patch";
        } else if (link.getURL().contains("git.kernel.org") || link.getURL().contains("source.codeaurora.org")) {
            return link.getURL().replaceAll("commit", "patch");
        } else if (link.getURL().contains("android.googlesource.com")) {
            return link.getURL() + "?format=TEXT"; //BASE64 ENCODED
        } else if (link.getURL().contains("review.lineageos.org") && !link.getURL().contains("topic")) {
            String id = link.getURL().split("/")[3];
            //TODO: Dynamically get revision
            return "https://review.lineageos.org/changes/" + id + "/revisions/1/patch?download"; //BASE64 ENCODED
        }
        return "NOT A PATCH";
    }

    private static boolean isBase64Encoded(Link link) {
        if (link.getURL().contains("android.googlesource.com") || link.getURL().contains("review.lineageos.org")) {
            return true;
        }
        return false;
    }

    private static String getPatchVersion(CVE cve, Link link) {
        String result = "";
        for (String version : linuxVersions) {
            if (link.getDesc().contains(version)) {
                result = version;
            }
        }
        if (result.length() == 0) {
            result = "ANY";
        }
        return result;
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
        private String notes;
        private ArrayList<Link> links;

        public CVE(String id, String notes, ArrayList<Link> links) {
            this.id = id;
            this.notes = notes;
            this.links = links;
        }

        public String getId() {
            return id;
        }

        public String getNotes() {
            return notes;
        }

        public ArrayList<Link> getLinks() {
            return links;
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
