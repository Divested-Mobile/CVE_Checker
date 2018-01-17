public class LinuxIncrs {

    private static final String compression = ".xz";

    public static void generateScript(String version, String patchLevel, int mostRecentVersion) {
        String url = "https://cdn.kernel.org/pub/linux/kernel/v" + version + "/incr/";
        System.out.println("#!/bin/bash");
        for (int x = 1; x < mostRecentVersion; x++) {
            String incr = x + "-" + (x + 1);
            String incrPad = String.format("%04d", x) + "-" + String.format("%04d", (x + 1));
            System.out.println("wget " + url + "patch-" + patchLevel + incr + compression + " -O - | xz -d > " + patchLevel + incrPad + ".patch");
        }
    }

}
