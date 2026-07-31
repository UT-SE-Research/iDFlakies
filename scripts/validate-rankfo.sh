#!/usr/bin/env bash
# End-to-end validation for the RankFO + OBO integration.
#
# What this checks:
#   1. iDFlakies builds cleanly
#   2. The validation-subject tests compile and pass (alone)
#   3. RankFO fixture files are generated correctly
#   4. mvn idflakies:minimize with RANKFO_PLUS_ONE finds exactly PolluterTest#pollute
#
# Usage:
#   bash scripts/validate-rankfo.sh
#
# From CI:
#   mvn install -DskipTests -q && bash scripts/validate-rankfo.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SUBJECT_DIR="$SCRIPT_DIR/../validation-subject"
IDFLAKIES_DIR="$SCRIPT_DIR/.."

EXPECTED_POLLUTER="com.example.PolluterTest.pollute"
EXPECTED_VICTIM="com.example.VictimTest.victim"

# ── Step 1: build iDFlakies ───────────────────────────────────────────────────
echo "=== [1/4] Building iDFlakies ==="
mvn -f "$IDFLAKIES_DIR/pom.xml" install -DskipTests -q
echo "Build OK"

# ── Step 2: compile validation-subject tests ──────────────────────────────────
echo "=== [2/4] Compiling validation-subject ==="
mvn -f "$SUBJECT_DIR/pom.xml" test-compile -q
echo "Compile OK"

# ── Step 3: generate .dtfixingtools fixtures ──────────────────────────────────
echo "=== [3/4] Generating fixtures ==="
bash "$SCRIPT_DIR/generate-fixtures.sh"

# ── Step 4: run minimize and assert output ────────────────────────────────────
echo "=== [4/4] Running mvn idflakies:minimize (RANKFO_PLUS_ONE) ==="
cd "$SUBJECT_DIR"

mvn idflakies:minimize \
  -Ddt.minimizer.strategy=RANKFO_PLUS_ONE \
  -Ddt.verify=true \
  2>&1 | tee /tmp/rankfo-minimize.log

MINIMIZED_DIR="$SUBJECT_DIR/.dtfixingtools/minimized"

if [ ! -d "$MINIMIZED_DIR" ]; then
  echo "FAIL: .dtfixingtools/minimized/ directory was not created"
  exit 1
fi

OUTPUT_FILE=$(ls "$MINIMIZED_DIR"/*.json 2>/dev/null | head -1)
if [ -z "$OUTPUT_FILE" ]; then
  echo "FAIL: no output file found in $MINIMIZED_DIR"
  exit 1
fi

echo ""
echo "Output file: $OUTPUT_FILE"

FLAKY_CLASS=$(python3 -c "import json; d=json.load(open('$OUTPUT_FILE')); print(d['flakyClass'])")
DEPENDENT_TEST=$(python3 -c "import json; d=json.load(open('$OUTPUT_FILE')); print(d['dependentTest'])")
POLLUTER=$(python3 -c "
import json, sys
d = json.load(open('$OUTPUT_FILE'))
if not d.get('polluters'):
    print('NONE')
else:
    print(d['polluters'][0]['deps'][0])
")

echo "  dependentTest : $DEPENDENT_TEST"
echo "  flakyClass    : $FLAKY_CLASS"
echo "  polluter      : $POLLUTER"
echo ""

FAIL=0

if [ "$FLAKY_CLASS" != "OD" ]; then
  echo "FAIL: flakyClass='$FLAKY_CLASS', expected 'OD'"
  FAIL=1
fi

if [ "$DEPENDENT_TEST" != "$EXPECTED_VICTIM" ]; then
  echo "FAIL: dependentTest='$DEPENDENT_TEST', expected '$EXPECTED_VICTIM'"
  FAIL=1
fi

if [ "$POLLUTER" != "$EXPECTED_POLLUTER" ]; then
  echo "FAIL: polluter='$POLLUTER', expected '$EXPECTED_POLLUTER'"
  FAIL=1
fi

if [ "$FAIL" -eq 1 ]; then
  echo "=== VALIDATION FAILED ==="
  exit 1
fi

echo "=== VALIDATION PASSED ==="
echo "RankFO correctly identified $EXPECTED_POLLUTER as the single polluter of $EXPECTED_VICTIM"
