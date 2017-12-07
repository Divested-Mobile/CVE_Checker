import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

public class Sorter {

    private static ArrayList<CVE> cves = new ArrayList<>();

    public static void main(String[] args) {
        try {
            Scanner s = new Scanner(new File("/home/***REMOVED***/Downloads/Kernel_CVE_Patch_List.txt"));
            String curId = "";
            ArrayList<String> lines = new ArrayList<String>();
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.startsWith("#")) {

                } else if (line.startsWith("CVE") || line.startsWith("LVT")) {
                    if (curId.length() > 0) {
                        //System.out.println("Added a new CVE - " + curId);
                        cves.add(new CVE(curId, lines));
                        curId = "";
                        lines = new ArrayList<String>();
                    }
                    curId = line;
                } else {
                    lines.add(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.sort(cves, new Comparator<CVE>() {
            @Override public int compare(CVE c1, CVE c2) {
                String[] c1s = c1.getId().split("-");
                Double c1d = Double.valueOf(c1s[1] + "." + c1s[2]);
                String[] c2s = c2.getId().split("-");
                Double c2d = Double.valueOf(c2s[1] + "." + c2s[2]);
                return c1d.compareTo(c2d);
            }
        });
        for (CVE cve : cves) {
            System.out.println(cve.getId());
            for (String line : cve.getLines()) {
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
    }

}



