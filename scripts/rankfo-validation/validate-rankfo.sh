#!/usr/bin/env bash
# Usage: bash scripts/rankfo-validation/validate-rankfo.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
IDFLAKIES_DIR="$SCRIPT_DIR/../.."
SUBJECT_DIR="$IDFLAKIES_DIR/validation-subject"

STRATEGIES=(RANKFO_PLUS_ONE RANKFO_METHODS RANKFO_DISTANCE_D RANKFO_COMBINED_P1_D RANKFO_COMBINED_M_D)

echo "=== [1/5] Verifying verify_scores.py itself against hand-computed toy inputs ==="
python3 "$SCRIPT_DIR/test_verify_scores.py"

echo "=== [2/5] Building iDFlakies (running idflakies-core unit tests) ==="
mvn -f "$IDFLAKIES_DIR/pom.xml" install -q

echo "=== [3/5] Compiling validation-subject ==="
mvn -f "$SUBJECT_DIR/pom.xml" test-compile -q

echo "=== [4/5] Generating fixtures (4 tests, 10 sampled orders) ==="
bash "$SCRIPT_DIR/generate-fixtures.sh"

echo "=== [5/5] Running minimize + verifying scores for all ${#STRATEGIES[@]} heuristics ==="
FAIL=0
for STRATEGY in "${STRATEGIES[@]}"; do
  echo "--- $STRATEGY ---"
  (cd "$SUBJECT_DIR" && mvn idflakies:minimize -Ddt.minimizer.strategy="$STRATEGY" -Ddt.verify=true) \
    2>&1 | tee "/tmp/rankfo-minimize-$STRATEGY.log"

  if ! python3 "$SCRIPT_DIR/verify_scores.py" --strategy "$STRATEGY"; then
    FAIL=1
  fi
done

[ "$FAIL" -eq 1 ] && echo "VALIDATION FAILED" && exit 1
echo "VALIDATION PASSED"
