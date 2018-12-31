#!/usr/bin/env bash

if [[ -z "$1" ]]; then
    echo "Usage: bash rebuild-detection-rounds.sh DETECTION_RESULTS"
    exit 1
fi

detection_results="$1"
scripts_folder=$(cd "$(dirname $BASH_SOURCE)"; pwd)

if [[ ! "$detection_results" =~ "$/" ]]; then
    detection_results="$(cd "$(dirname $detection_results)"; pwd)/$(basename $detection_results)"
fi

# Go to where the pom is
cd "$scripts_folder/.."

mvn install -DskipTests exec:java -Dexec.mainClass="edu.illinois.cs.dt.tools.detection.RebuildDetectionRounds" \
    -Dexec.args="--results '$detection_results'"

