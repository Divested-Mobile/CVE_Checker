/*
 * Copyright (c) 2017-2018 Divested Computing Group
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
public class LinuxIncrsGen {

  public static void generateScript(String version, int mostRecentVersion) {
    System.out.println("#!/bin/bash");
    for (int x = 1; x < mostRecentVersion; x++) {
      String incr = x + "-" + (x + 1);
      String incrPad = String.format("%04d", x) + "-" + String.format("%04d", (x + 1));

      System.out.println("git checkout v" + version + "." + (x + 1));
      System.out.println("git format-patch --stdout v" + version + "." + x + " > $incrPath/"
          + version + "/" + version + "." + incrPad + ".patch");
    }
  }

}
