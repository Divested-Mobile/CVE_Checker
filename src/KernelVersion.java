public class KernelVersion {

    private String versionFull = "";
    private int version = 0;
    private int patchLevel = 0;

    public KernelVersion(String version) {
        this.versionFull = version;
        String[] versionSplit = version.split("\\.");
        this.version = Integer.valueOf(versionSplit[0]);
        this.patchLevel = Integer.valueOf(versionSplit[1]);
    }

    public KernelVersion(int version, int patchLevel) {
        this.versionFull = version + "." + patchLevel;
        this.version = version;
        this.patchLevel = patchLevel;
    }

    public String getVersionFull() {
        return versionFull;
    }

    public int getVersion() {
        return version;
    }

    public int getPatchLevel() {
        return patchLevel;
    }

    public boolean isGreaterVersion(KernelVersion comparedTo) {
        if (getVersion() > comparedTo.getVersion()) {
            return true;
        }
        return getVersion() == comparedTo.getVersion() && getPatchLevel() >= comparedTo.getPatchLevel();
    }

    public boolean isLesserVersion(KernelVersion comparedTo) {
        if (getVersion() < comparedTo.getVersion()) {
            return true;
        }
        return getVersion() == comparedTo.getVersion() && getPatchLevel() <= comparedTo.getPatchLevel();
    }
}
