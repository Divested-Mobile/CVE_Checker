/*
Copyright (c) 2022-2024 Divested Computing Group

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Common {
    public static final String URL_LINUX_MAINLINE =
            "https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/commit/?id=";
    public static final String URL_LINUX_STABLE =
            "https://git.kernel.org/pub/scm/linux/kernel/git/stable/linux.git/commit/?id=";
    public static final String URL_LINUX_CIP =
            "https://git.kernel.org/pub/scm/linux/kernel/git/cip/linux-cip.git/commit/?id=";
    public static final String URL_AOSP_STABLE =
            "https://android.googlesource.com/kernel/common/+/";
    public static String INCLUSIVE_KERNEL_PATH = null;

    public static void initEnv() {
        if(System.getenv("DOS_PATCHER_INCLUSIVE_KERNEL") != null) {
            if(new File(System.getenv("DOS_PATCHER_INCLUSIVE_KERNEL")).exists()) {
                INCLUSIVE_KERNEL_PATH = System.getenv("DOS_PATCHER_INCLUSIVE_KERNEL");
            }
        }
    }

    public static int runCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            while (process.isAlive()) {
                // Do nothing
            }
            return process.exitValue();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static Version getKernelVersion(File kernelMakefile) {
        try {
            return getKernelVersion(new Scanner(new File(kernelMakefile + "/Makefile")), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Version getKernelVersion(Scanner kernelMakefile, boolean quiet) {
        String kernelVersion = "";
        try {
            while (kernelMakefile.hasNextLine()) {
                String line = kernelMakefile.nextLine().trim();
                if (line.startsWith("VERSION = ")) {
                    kernelVersion = line.split("= ")[1];
                }
                if (line.startsWith("PATCHLEVEL = ")) {
                    kernelVersion += "." + line.split("= ")[1];
                }
/*                if (line.startsWith("SUBLEVEL = ")) {
                    if(!line.split("= ")[1].equals("0")) {
                        kernelVersion += "." + line.split("= ")[1];
                    }
                }*/
                if (line.startsWith("NAME = ")) {
                    break;
                }
            }
            kernelMakefile.close();
            if(!quiet) {
                System.out.println("Detected kernel version " + kernelVersion);
            }
            return new Version(kernelVersion);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Version getPatchVersion(String commitID) {
        if(INCLUSIVE_KERNEL_PATH == null) {
            System.out.println("Kernel repo unavailable!");
            System.exit(1);
        }
        try {
            ProcessBuilder gitShow = new ProcessBuilder("git", "-C", INCLUSIVE_KERNEL_PATH, "show", commitID + ":Makefile");
            Process gitShowExec = gitShow.start();
/*            if(!gitShowExec.waitFor(100, TimeUnit.MILLISECONDS)) {
                gitShowExec.destroy();
                return null;
            }*/
/*            if (gitShow.exitValue() != 0) {
                System.out.println("Failed to get patch version " + commitID);
                System.exit(1);
            }*/
            Scanner output = new Scanner(gitShowExec.getInputStream());
            return getKernelVersion(output, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
