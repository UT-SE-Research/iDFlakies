#!/usr/bin/env bash

# Usage: bash update-fix-method-order.sh DATABASE

if [[ $1 == "" ]]; then
    echo "arg1 - Path to database to update"
    exit
fi

database="$1"

echo "[INFO] Updating FixMethodOrder info"
QUERY="select sr.sha, s.slug, ftc.subject_name, ftc.test_name from flaky_test_classification ftc join subject s on ftc.subject_name = s.name join subject_raw sr on lower(sr.slug) = lower(s.slug);"
echo "$QUERY" | sqlite3 "$database" | while read line; do
    sha="$(echo "$line" | cut -d'|' -f1)"
    slug="$(echo "$line" | cut -d'|' -f2)"
    subject_name="$(echo "$line" | cut -d'|' -f3)"
    test_name="$(echo "$line" | cut -d'|' -f4-)"
    # Get everything before the last '.'
    test_class_name=$(echo "$test_name" | rev | cut -f2 -d"." | rev)

    if [[ ! -d "temp-subject/$slug" ]]; then
        mkdir -p "temp-subject"
        git clone "https://github.com/$slug" "temp-subject/$slug"
    fi

    (
        cd "temp-subject/$slug"
        git checkout "$sha"
    ) &> /dev/null

    if find "temp-subject/$slug" -name "$test_class_name.java" | xargs grep -q "@FixMethodOrder"; then
        echo "@FixMethodOrder found: $test_name"
        # echo "update original_order set fix_method_order = 1 where test_name = '$test_name' and subject_name='$subject_name';" | sqlite3 "$database"
    else
        echo "@FixMethodOrder NOT found: $test_name"
        # echo "update original_order set fix_method_order = 0 where test_name = '$test_name' and subject_name='$subject_name';" | sqlite3 "$database"
    fi
done

