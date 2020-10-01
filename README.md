DivestOS CVE Patcher
====================

A tool for downloading, checking, and applying (CVE) patches to a (kernel) repository.

Credits
-------
- Thanks to @z3ntu for Gradle build support
- David Koelle for AlphanumComparator, http://www.davekoelle.com/alphanum.html

Quick Start
-----------
- Clone this repo, cd into it, and compile the tool: gradle jar
- Put the resulting jar into your .bashrc: $DOS_BINARY_PATCHER
- Clone the patches repo, put it into your .bashrc: $DOS_PATCHES_LINUX_CVES

Adding Patches
--------------
- This is a manual process. CVEs are sourced from the sources listed at the top of Kernel_CVE_Patch_List.txt

Importing CIP Patches
---------------------
- Run: ./CIP.sh $PATH_TO_CIP_REPO
- Then run: git diff CIP.txt
- Manually import the new patches into Kernel_CVE_Patch_List.txt

Downloading Patches
-------------------
- If updating an existing patchset, rm -rf it first
- Then: java -jar patcher.jar download $DOS_PATCHES_LINUX_CVES/Kernel_CVE_Patch_List.txt

Downloading Entire Repository
-----------------------------
- This will take 1-2 hours
- You will likely be rate-limited
- Some patches will be missing as the links may no longer be valid
- There are a handful of patches that have been added by hand (eg. compressed, or manually backported)

Patching
--------
- To patch a kernel directly: java -jar $DOS_BINARY_PATCHER patch direct $DOS_PATCHES_LINUX_CVES $outputDir/ $repoPath/:repoName...
- To patch a kernel in workspace: java -jar $DOS_BINARY_PATCHER patch workspace $workspace/ $DOS_PATCHES_LINUX_CVES $outputDir/ repoName...

Using the Resulting Scripts
---------------------------
- This part is entirely up to you
- They are intended to be run during build time
- The results of them shouldn't be commited to a tree due to the automated nature

Identifying Failed Patches
--------------------------
- During compile-time there is an obvious chance it will fail
- Take the error
- cd into $DOS_PATCHES_LINUX_CVES
- Use rg -l $snippet_of_error
- Check to see if any of those patches were applied
- Then look at each applied patch to narrow it down
- Once you find it, you'll want to mark that somewhere. DivestOS has a Fix_CVE_Patchers.sh for tracking/disabling them
- Generally if it compiles, it boots. However there are patches that can compile and absolutely break boot, see: CVE-2017-13218/4.4/0026.patch
