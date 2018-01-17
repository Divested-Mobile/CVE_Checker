import java.io.File;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class RepoExtractor {

    public static void extract(File manifest) {
        try {
            Scanner s = new Scanner(manifest);
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
