#!/usr/bin/env bash

# Usage: bash download-dt-list.sh
# Should be run from the root level of the github repo you are running on

for pom_path in $(find -name "pom.xml"); do
    dir=$(dirname $pom_path)
    module_key=$(/home/awshi2/dt-fixing-tools/scripts/docker/module-key $(pwd) $dir)

    mkdir -p "$dir/.dtfixingtools/detection-results/"
    echo "[DOWNLOAD] Downloading $module_key to $dir/.dtfixingtools/detection-results/flaky-lists.json"
    wget "http://reedoei.com/files/${module_key}-flaky-lists.json" -O "$dir/.dtfixingtools/detection-results/flaky-lists.json"
done

