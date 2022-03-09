import java.io.IOException;

public class Common {
    public static final String URL_LINUX_MAINLINE =
            "https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/commit/?id=";
    public static final String URL_LINUX_STABLE =
            "https://git.kernel.org/pub/scm/linux/kernel/git/stable/linux.git/commit/?id=";
    public static final String URL_AOSP_STABLE =
            "https://android.googlesource.com/kernel/common/+/";

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

}
