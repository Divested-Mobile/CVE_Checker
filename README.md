DivestOS CVE Patcher
====================

A tool for downloading, checking, and applying (CVE) patches to a (kernel) repository.

Notes on CVE Patching
----------------------
- Patches applied may not be relevant to a device's architecture or hardware
- Patches can make issues worse, or create new issues
- Backported patches do not receive much review
- Patches may not completely mitigate the issue they intend to
- There are many security patches that do not receive CVEs
- Linux has many known security issues that go unresolved for years
- This is not a long-term solution
- We need more rigorous support lifecycles from upstreams
- This project is solely made to attempt to improve the security and by extension the lifespan of unsupported devices

Real World Use
--------------
- This project was considered viable by end of 2017 and has been in use since then for DivestOS.
- The corresponding CVE database is likely one of the largest with support for older kernels.
- It is currently used for improving the security of the 100+ devices supported by DivestOS, and is tested booting on 65+ of them.
- It is often near impossible to provide viable mainline support for many vendor altered kernel trees. We believe this project at the very least improves their situation. To ignore this is to be a defeatist. Not everyone can afford the latest shiny thing.
- Applied examples: [19.1](https://github.com/Divested-Mobile/DivestOS-Build/tree/master/Scripts/LineageOS-19.1/CVE_Patchers), [18.1](https://github.com/Divested-Mobile/DivestOS-Build/tree/master/Scripts/LineageOS-18.1/CVE_Patchers), [17.1](https://github.com/Divested-Mobile/DivestOS-Build/tree/master/Scripts/LineageOS-17.1/CVE_Patchers), [16.0](https://github.com/Divested-Mobile/DivestOS-Build/tree/master/Scripts/LineageOS-16.0/CVE_Patchers), [15.1](https://github.com/Divested-Mobile/DivestOS-Build/tree/master/Scripts/LineageOS-15.1/CVE_Patchers), [14.1](https://github.com/Divested-Mobile/DivestOS-Build/tree/master/Scripts/LineageOS-14.1/CVE_Patchers)

Patch Database
--------------
- [On GitHub](https://github.com/Divested-Mobile/Kernel_Patches) or [On GitLab](https://gitlab.com/divested-mobile/kernel_patches)

Prebuilts
---------
- via CI: https://gitlab.com/divested-mobile/cve_checker/-/jobs/artifacts/master/browse?job=build

License
-------
- GPL-3.0-or-later
- Or at your discretion: GPL-2.0 (as per the kernel itself)

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
- Pointing $DOS_PATCHER_INCLUSIVE_KERNEL to a (CIP) combined kernel repo will generate patches locally when possible

Patching
--------
- Key: $outputDir is where script will be saved, $repoPath is the kernel to be checked, $repoName is vanity name of kernel
- To patch a kernel directly: java -jar $DOS_BINARY_PATCHER patch direct $DOS_PATCHES_LINUX_CVES $outputDir/ $repoPath/:repoName...
- To patch a kernel in an AOSP workspace: java -jar $DOS_BINARY_PATCHER patch workspace $workspace/ $DOS_PATCHES_LINUX_CVES $outputDir/ $repoName...

Using the Resulting Scripts
---------------------------
- This part is entirely up to you
- They are intended to be run during build time
- The results of them shouldn't be committed to a tree due to the automated nature

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

Patch Version Matrix
--------------------
| Version | Default | Loose | Extreme         | Reverse |
| ------- | ------- | ----- | --------------- | ------- |
| 3.0     |3.0      | 3.4   | 3.10, 3.18, 4.4 | x       |
| 3.4     |3.4      | 3.10  | 3.18, 4.4       | x       |
| 3.10    |3.10     | 3.18  | 4.4             | 3.4     |
| 3.18    |3.18     | 4.4   | 4.9             | x       |
| 4.4     |4.4      | 4.9   | x               | x       |
| 4.9     |4.9      | 4.14  | x               | x       |

Relevant Links
--------------
- https://gitlab.com/cip-project/cip-kernel/cip-kernel-sec
- https://raw.githubusercontent.com/ossf/wg-securing-critical-projects/main/presentations/The_state_of_the_Linux_kernel_security.pdf
- https://www.youtube.com/watch?v=F_Kza6fdkSU
- https://github.com/android-linux-stable
- https://github.com/raymanfx/android-cve-checker
- https://github.com/tdm/vuln-patcher

Implementation Discussions + Examples
-------------------------------------
- https://github.com/AsteroidOS/asteroid/issues/165
- https://github.com/AsteroidOS/meta-bass-hybris/pull/16
- https://github.com/AsteroidOS/meta-sawfish-hybris/pull/2
- https://github.com/Geofferey/omni_kernel_oneplus_sm8150/pull/1
- https://github.com/GrapheneOS-Archive/kernel_google_marlin/pull/1
- https://github.com/hashbang/os/issues/43
- https://github.com/HelloVolla/android_kernel_volla_mt6763/pull/10
- https://github.com/HelloVolla/android_kernel_volla_mt6763/pull/8
- https://github.com/NixOS/mobile-nixos/issues/383
- https://github.com/NixOS/mobile-nixos/pull/384
- https://github.com/the-modem-distro/quectel_eg25_kernel/pull/7
- https://github.com/ubports/ubuntu-touch/issues/1566
- https://github.com/voron00/android_kernel_lge_mako/pull/1
- https://gitlab.com/calyxos/calyxos/-/issues/205
- https://gitlab.com/LineageOS/issues/devrel/-/issues/235
- https://gitlab.com/postmarketOS/pmbootstrap/-/issues/1746

Donate
-------
- https://divested.dev/donate
