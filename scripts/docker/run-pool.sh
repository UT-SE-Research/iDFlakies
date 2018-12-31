#!/usr/bin/env bash

if [[ $1 == "" ]] || [[ $2 == "" ]] || [[ $3 == "" ]]; then
    echo "arg1 - Path to CSV file with project,sha"
    echo "arg2 - Number of rounds"
    echo "arg3 - Timeout in seconds"
    echo "arg4 - The script to run (Optional)"
    echo "arg5 - Number of processes to run at the same time (Optional)"
    exit
fi

PROCESS_NUM="$5"

if [[ -z "$PROCESS_NUM" ]]; then
    PROCESS_NUM="4"
fi

find "$1" -maxdepth 1 -type f -name "*.csv" | xargs -P"$PROCESS_NUM" -I{} bash run-project-pool.sh {} "$2" "$3" "$4"

