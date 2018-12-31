#!/usr/bin/env bash

# Usage: bash generate_all_results.sh <database>

scripts_folder=$(cd "$(dirname $BASH_SOURCE)"; pwd)

database="$1"

sqlite3 -header -column "$database" < "$scripts_folder/../src/main/sql/full_results.sql"
