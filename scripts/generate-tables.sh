#!/usr/bin/env bash

# Usage: bash generate-commands.sh DATABASE
database="$1"

scripts_folder=$(cd "$(dirname $BASH_SOURCE)"; pwd)

if [[ ! "$database" =~ "$/" ]]; then
    database="$(cd "$(dirname $database)"; pwd)/$(basename $database)"
fi

# Go to where the pom is
cd "$scripts_folder/.."

mvn install -DskipTests exec:java -Dexec.mainClass="edu.illinois.cs.dt.tools.detection.analysis.TableGenerator" -Dexec.args="--db '$database'"

