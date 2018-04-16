/*
Copyright (c) 2017-2018 Spot Communications, Inc.

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
