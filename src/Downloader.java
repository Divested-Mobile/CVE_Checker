import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class Downloader {

    private static String cveJson = "/home/***REMOVED***/Downloads/cves";
    private static String output = "/mnt/Drive-1/Development/Other/Android_ROMs/Patches/Linux_CVEs-New";
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

        for(CVE cve : cves) {
            //Only run if we have patches available
            if(cve.getLinks().size() > 0) {
                //Iterate over all links and download if needed
                for (Link link : cve.getLinks()) {

                }
            }
        }
    }

    private static String getPatchURL(Link link) {
        if(link.getURL().contains("github.com")) {
            return link.getURL() + ".patch";
        } else if(link.getURL().contains("git.kernel.org") || link.getURL().contains("source.codeaurora.org")) {
            return link.getURL().replaceAll("commit", "patch");
        } else if(link.getURL().contains("android.googlesource.com")) {
            return "";
        } else if(link.getURL().contains("review.lineageos.org")) {
            return "";
        }
        return "";
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
