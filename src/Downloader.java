import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Scanner;

public class Downloader {

    private static final String[] linuxVersions = new String[] {"3.0", "3.2", "3.4", "3.8", "3.10", "3.12", "3.16", "3.18", "4.4", "4.5", "4.8"};
    private static String cveJson = "/home/***REMOVED***/Downloads/cves"; //https://cve.lineageos.org/api/v1/cves
    private static String output = "/mnt/Drive-1/Development/Other/Android_ROMs/Patches/Linux_CVEs-New/";
    private static ArrayList<CVE> cves = new ArrayList<CVE>();

    public static void main(String[] args) {
        //Read in all the CVEs from the JSON file
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
                    if (lineS.length > 3) {
                        curNotes = lineS[3];
                        System.out.println("\t\tAdded a new note: " + curNotes);
                    }
                }
            }
            cve.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Downloading patches...");
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
                        String patchOutput = outDir.getAbsolutePath() + "/" + linkC + ".patch" + base64;
                        downloadFile(patch, new File(patchOutput), true);
                        if (isBase64Encoded(link)) {
                            try {
                                Process b64dec = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", "base64 -d " + patchOutput + " > " + patchOutput.replaceAll(base64, "")});
                                while (b64dec.isAlive()) {
                                    //Do nothing
                                }
                                if (b64dec.exitValue() != 0) {
                                    System.exit(1);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("\t\tDownloaded " + link.getURL());
                        linkC++;
                    }
                }
            }
        }
    }

    private static String getPatchURL(Link link) {
        String url = link.getURL().replaceAll("http://", "https://");
        if (url.contains("github.com")) {
            return url + ".patch";
        } else if (url.contains("git.kernel.org")) {
            return url.replaceAll("cgit/", "pub/scm/").replaceAll("commit", "patch");
        } else if (url.contains("source.codeaurora.org")) {
            return url.replaceAll("commit", "patch");
        } else if (url.contains("android.googlesource.com")) {
            String add = "";
            if (!url.contains("%5E%21")) {
                add += "%5E%21/";
            }
            add += "?format=TEXT";
            return url.replaceAll("/#F0", "") + add; //BASE64 ENCODED
        } else if (url.contains("review.lineageos.org") && !url.contains("topic") && !url.contains("#/q")) {
            int idS = 3;
            if (url.contains("#/c")) {
                idS = 5;
            }
            String id = url.split("/")[idS];
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
        String note = cve.getNotes().toLowerCase();
        String result = "";
        for (String version : linuxVersions) {//Gather version from link description
            if (link.getDesc().contains(version)) {
                result = version;
            }
        }
        for (String version : linuxVersions) {//Gather version from note
            if ((note.startsWith("kernel before " + version) && note.length() <= 21)
                || (note.startsWith("kernel up to " + version) && note.length() <= 20)
                || (note.startsWith("kernel < " + version) && note.length() <= 16)
                || (note.startsWith("linux kernel before " + version) && note.length() <= 27)
                || (note.startsWith("kernel through " + version) && note.length() <= 22)) {
                if (result.length() > 0) {
                    result += "-";
                }
                result += "<" + version;
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
