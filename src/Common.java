import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Common {
    public static final String URL_LINUX_MAINLINE =
            "https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/commit/?id=";
    public static final String URL_LINUX_STABLE =
            "https://git.kernel.org/pub/scm/linux/kernel/git/stable/linux.git/commit/?id=";
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
            Process gitShow = Runtime.getRuntime().exec("git -C " + INCLUSIVE_KERNEL_PATH + " show " + commitID + ":Makefile");
            if(!gitShow.waitFor(100, TimeUnit.MILLISECONDS)) {
                gitShow.destroy();
                return null;
            }
/*            if (gitShow.exitValue() != 0) {
                System.out.println("Failed to get patch version " + commitID);
                System.exit(1);
            }*/
            Scanner output = new Scanner(gitShow.getInputStream());
            return getKernelVersion(output, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
