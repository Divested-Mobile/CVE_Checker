public class Version {

    private String versionFull = "";
    private int version = 0;
    private int patchLevel = 0;

    public Version(String version) {
        this.versionFull = version;
        String[] versionSplit = version.split("\\.");
        this.version = Integer.valueOf(versionSplit[0]);
        this.patchLevel = Integer.valueOf(versionSplit[1]);
    }

    public Version(int version, int patchLevel) {
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

    public boolean isGreaterVersion(Version comparedTo, boolean ignoreMajor) {
        if (getVersion() > comparedTo.getVersion()) {
            return true;
        }
        if(!ignoreMajor) {
            return getVersion() == comparedTo.getVersion() && getPatchLevel() >= comparedTo.getPatchLevel();
        } else {
            return getVersion() >= comparedTo.getVersion() && getPatchLevel() >= comparedTo.getPatchLevel();
        }
    }

    public boolean isLesserVersion(Version comparedTo, boolean ignoreMajor) {
        if (getVersion() < comparedTo.getVersion()) {
            return true;
        }
        if(!ignoreMajor) {
            return getVersion() == comparedTo.getVersion() && getPatchLevel() <= comparedTo.getPatchLevel();
        } else {
            return getVersion() <= comparedTo.getVersion() && getPatchLevel() <= comparedTo.getPatchLevel();
        }
    }
}
