import java.io.File;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class RepoExtractor {

    public static void main(String[] args) {
        try {
            Scanner s = new Scanner(new File("/mnt/Drive-1/Development/Other/Android_ROMs/Patches/Android/Android_CVEs.txt"));
            Set<String> repos = new HashSet<String>();
            while(s.hasNextLine()) {
                String line = s.nextLine();
                if(line.contains("googlesource")) {
                    line = line.split("platform/")[1];
                    line = line.split("/\\+/")[0];
                    line = line.replaceAll("/", "_");
                    repos.add(line);
                }
            }
            s.close();
            for(String repo : repos) {
                System.out.println(repo);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
