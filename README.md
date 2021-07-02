DivestOS CVE Patcher
====================

A tool for downloading, checking, and applying (CVE) patches to a (kernel) repository.

Notes on CVE Patching
----------------------
- Patches applied may not be relevant to a device's architecture or hardware
- Patches can make issues worse, or create new issues
- Backported patches do not receive much review
- Patches may not compeletely mitigate the issue they intend to
- There are many security patches that do not receive CVEs
- Linux has many known security issues that go unresolved for years
- This is not a long-term solution
- We need more rigrious support lifecycles from upstreams
- This project is soley made to attempt to improve the security and by extension the lifespan of unsupported devices

Real World Use
--------------
- This project was considered viable by end of 2017 and has been in use since then for DivestOS.
- The corresponding CVE database is likely one of the largest with support for older kernels.
- It is currently used for improving the security of the 80+ devices supported by DivestOS, and is tested booting on 30+ of them.
- It is often near impossible to provide viable mainline support for many vendor altered kernel trees. We believe this project at the very least improves their sitatuion. To ignore this is to be a defeatist. Not everyone can afford the latest shiny thing.
- Applied examples: [18.1](https://github.com/Divested-Mobile/DivestOS-Build/tree/master/Scripts/LineageOS-18.1/CVE_Patchers), [17.1](https://github.com/Divested-Mobile/DivestOS-Build/tree/master/Scripts/LineageOS-17.1/CVE_Patchers), [16.0](https://github.com/Divested-Mobile/DivestOS-Build/tree/master/Scripts/LineageOS-16.0/CVE_Patchers), [15.1](https://github.com/Divested-Mobile/DivestOS-Build/tree/master/Scripts/LineageOS-15.1/CVE_Patchers), [14.1](https://github.com/Divested-Mobile/DivestOS-Build/tree/master/Scripts/LineageOS-14.1/CVE_Patchers)

Patch Database
--------------
- [On GitHub](https://github.com/Divested-Mobile/Kernel_Patches) or [On GitLab](https://gitlab.com/divested-mobile/kernel_patches)

Credits
-------
- Thanks to @z3ntu for Gradle build support
- David Koelle for AlphanumComparator (MIT), http://www.davekoelle.com/alphanum.html

Quick Start
-----------
- Clone this repo, cd into it, and compile the tool: gradle jar
- Put the resulting jar into your .bashrc: $DOS_BINARY_PATCHER
- Clone the patches repo, put it into your .bashrc: $DOS_PATCHES_LINUX_CVES

Adding Patches
--------------
- This is a manual process. CVEs are sourced from the sources listed at the top of [Kernel_CVE_Patch_List.txt](https://raw.githubusercontent.com/Divested-Mobile/Kernel_Patches/master/Kernel_CVE_Patch_List.txt)

Importing CIP Patches
---------------------
- Run: ./CIP.sh $PATH_TO_CIP_REPO
- Run: git diff CIP.txt
- Manually import the new patches into Kernel_CVE_Patch_List.txt

Importing Linux incremental diffs
----------------------------------
- Open kernel.org in a browser
- Run: cd 0001-LinuxIncrementals/4.4
- Run: java -jar $DOS_BINARY_PATCHER linuxIncrDownload 4.x 4.4. 238 > download.sh
- Run: git diff download.sh
- Manually run the commands shown in the diff to download the new ones

Importing Linux incremental patches
------------------------------------
- Open kernel.org in a browser
- Run: cd 0001-LinuxIncrementals/4.4
- Run: java -jar $DOS_BINARY_PATCHER linuxIncrGen 4.4 238 > generate.sh
- Run: cd $somewhereElse && git clone https://git.kernel.org/pub/scm/linux/kernel/git/stable/linux.git && git fetch
- Run: export incrPath="$PATH_TO/0001-LinuxIncrementals"
- Run: sh $PATH_TO/generate.sh

Downloading Patches
-------------------
- If updating an existing patchset, rm -rf it first
- Run: java -jar patcher.jar download $DOS_PATCHES_LINUX_CVES/Kernel_CVE_Patch_List.txt

Downloading Entire Repository
-----------------------------
- This will take 1-2 hours
- You will likely be rate-limited
- Some patches will be missing as the links may no longer be valid
- There are a handful of patches that have been added by hand (eg. compressed, or manually backported)

Patching
--------
- To patch a kernel directly: java -jar $DOS_BINARY_PATCHER patch direct $DOS_PATCHES_LINUX_CVES $outputDir/ $repoPath/:repoName...
- To patch a kernel in an AOSP workspace: java -jar $DOS_BINARY_PATCHER patch workspace $workspace/ $DOS_PATCHES_LINUX_CVES $outputDir/ $repoName...

Using the Resulting Scripts
---------------------------
- This part is entirely up to you
- They are intended to be run during build time
- The results of them shouldn't be commited to a tree due to the automated nature

Identifying Failed Patches
--------------------------
- During compile-time there is an obvious chance it will fail
- Take the error
- Run: cd $DOS_PATCHES_LINUX_CVES
- Run: rg -l $snippet_of_error
- Check to see if any of those patches were applied
- Then look at each applied patch to narrow it down
- Once you find it, you'll want to mark that somewhere. DivestOS has a [Fix_CVE_Patchers.sh](https://github.com/Divested-Mobile/DivestOS-Build/blob/master/Scripts/Common/Fix_CVE_Patchers.sh) for tracking/disabling them
- Generally if it compiles, it boots. However there are patches that can compile and absolutely break boot, see: CVE-2017-13218/4.4/0027.patch

Relevant Links
--------------
- https://gitlab.com/cip-project/cip-kernel/cip-kernel-sec
- https://raw.githubusercontent.com/ossf/wg-securing-critical-projects/main/presentations/The_state_of_the_Linux_kernel_security.pdf
- https://www.youtube.com/watch?v=F_Kza6fdkSU
- https://github.com/android-linux-stable
- https://github.com/raymanfx/android-cve-checker
- https://github.com/tdm/vuln-patcher

Implementation Discussions
--------------------------
- https://github.com/hashbang/os/issues/43
- https://gitlab.com/postmarketOS/pmbootstrap/-/issues/1746
- https://github.com/ubports/ubuntu-touch/issues/1566
- https://github.com/AsteroidOS/asteroid/issues/165
- https://gitlab.com/calyxos/calyxos/-/issues/205
- https://gitlab.com/LineageOS/issues/devrel/-/issues/235
- https://github.com/GrapheneOS-Archive/kernel_google_marlin/pull/1

Donate
-------
BTC: bc1qkjtp2k7cc4kuv8k9wjdlxkeuqczenrpv5mwasl
