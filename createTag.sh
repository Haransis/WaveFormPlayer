#!/bin/bash
# Run this script for every new release
previousVersion=$(git describe --tags "$(git rev-list --tags --max-count=1)")
echo "Which version number will you choose for a new release ?"
read -r newVersion
echo "$newVersion"
echo "$previousVersion"
sed -i "s/$previousVersion/$newVersion/" README.md
sed -i "/versionName/s/$previousVersion/$newVersion/" soundwave/build.gradle
code=$(grep versionCode < soundwave/build.gradle | sed 's/.* //g')
newCode="$((code+1))"
sed -i "/versionCode/s/$code/$newCode/" soundwave/build.gradle
git add README.md soundwave/build.gradle
git commit -m ":pencil: Update version number"
git tag -a "$newVersion"