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

Patching
--------
- To patch a kernel directly: java -jar $DOS_BINARY_PATCHER patch direct $DOS_PATCHES_LINUX_CVES $outputDir/ $repoPath/:repoName...
- To patch a kernel in workspace: java -jar $DOS_BINARY_PATCHER patch workspace $workspace/ $DOS_PATCHES_LINUX_CVES $outputDir/ repoName...

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
