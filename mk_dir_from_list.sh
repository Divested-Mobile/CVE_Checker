#!/bin/bash
#Copyright (c) 2021 Divested Computing Group
#
#This program is free software: you can redistribute it and/or modify
#it under the terms of the GNU General Public License as published by
#the Free Software Foundation, either version 3 of the License, or
#(at your option) any later version.
#
#This program is distributed in the hope that it will be useful,
#but WITHOUT ANY WARRANTY; without even the implied warranty of
#MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#GNU General Public License for more details.
#
#You should have received a copy of the GNU General Public License
#along with this program.  If not, see <https://www.gnu.org/licenses/>.

patchIdentifier='Mon Sep 17 00:00:00 2001';
emailIdentifier='Thu Jan  1 00:00:00 1970';

list=$1;
incr=1;
if [ -f "$list" ]; then
	outDirectory=$(sed 's/.txt//' <<< $list);
	mkdir $outDirectory;
	while IFS= read -r line
	do
		currentWorkingPatch=$(eval "echo $line");
		if [ -f "$currentWorkingPatch" ]; then
			firstLine=$(head -n1 "$currentWorkingPatch");
			incrPadded=$(printf "%04d" $incr);
			if [[ "$firstLine" = *"$patchIdentifier"* ]]; then
				commitId=$(sed 's/From //' <<< $firstLine);
				commitId=$(sed "s/ $patchIdentifier//" <<< $commitId);
				commitId=$(head -c12 <<< $commitId);
				subject=$(sed 's/Subject: //' <<< $(sed -n 4p "$currentWorkingPatch"));
				subjectEscaped=$(tr -dc '[:alnum:][:blank:]\n\r' <<< "$subject");
				cp "$currentWorkingPatch" "$outDirectory/$incrPadded-$commitId-$subjectEscaped.patch";
			elif [[ "$firstLine" = *"$emailIdentifier"* ]]; then
				cp "$currentWorkingPatch" "$outDirectory/$incrPadded.patch";
			else
				cp "$currentWorkingPatch" "$outDirectory/$incrPadded.diff";
			fi;
			incr=$((incr + 1));
		fi;
	done < "$list";
fi;
