DivestOS CVE Patcher
====================

A tool for downloading, checking, and applying (CVE) patches to a repository.


Credits
-------
- Thanks to @z3ntu for Gradle build support
- David Koelle for AlphanumComparator, http://www.davekoelle.com/alphanum.html

Quick Start
-----------
- Clone this repo, cd into it, run to compile the tool: gradle jar
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
