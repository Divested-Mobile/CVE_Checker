public class LinuxIncrs {

    private static final String VERSION = "3.x";
    private static final String url = "https://cdn.kernel.org/pub/linux/kernel/v" + VERSION + "/incr/";
    private static final String compression = ".xz";

    private static final String patchLevel = "3.0.";
    private static final int mostRecentVersion = 101;

    public static void main(String[] args) {
        for(int x = 10; x < mostRecentVersion; x++) {
            String incr = x + "-" + (x + 1);
            String incrPad = String.format("%04d", x) + "-" + String.format("%04d", (x + 1));
            System.out.println("wget " + url + "patch-" + patchLevel + incr + compression + " -O - | xz -d > " + patchLevel + incrPad + ".patch");
        }
    }

}
