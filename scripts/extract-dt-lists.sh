#!/usr/bin/env bash

if [[ -z "$1" ]]; then
    echo "Usage: bash extract-dt-lists.sh DETECTION_RESULTS [OUTPUT]"

    exit 1
fi

detection_results="$1"
output_folder="$2"

scripts_folder=$(cd "$(dirname $BASH_SOURCE)"; pwd)

if [[ ! "$detection_results" =~ "$/" ]]; then
    detection_results="$(cd "$(dirname $detection_results)"; pwd)/$(basename $detection_results)"
fi

if [[ ! "$output_folder" =~ "$/" ]]; then
    output_folder="$(cd "$(dirname $output_folder)"; pwd)/$(basename $output_folder)"
fi

# Go to where the pom is
cd "$scripts_folder/.."

mvn install -DskipTests exec:java -Dexec.mainClass="edu.illinois.cs.dt.tools.detection.DependentTestExtractor" \
    -Dexec.args="--results '$detection_results' --output '$output_folder'"

