/*
Copyright (c) 2017-2018 Divested Computing, Inc.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
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
