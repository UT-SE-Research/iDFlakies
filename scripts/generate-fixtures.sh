#!/usr/bin/env bash
# Generates .dtfixingtools/ fixtures for the RankFO validation subject.
#
# Two orderings are generated so that PLUS_ONE heuristic ranks PolluterTest
# first (polluterScore=1.0, tied with InnocentTest, but stable insertion
# order puts Polluter first):
#
#   run0: [PolluterTest, InnocentTest, VictimTest]  V=FAILURE  (relevant VP)
#   run1: [InnocentTest, PolluterTest, VictimTest]  V=FAILURE  (relevant VP)
#
# Expected RankFO output: [PolluterTest, InnocentTest]
# Expected minimize result: polluters=[PolluterTest.pollute] found in 1 OBO step.

set -euo pipefail

SUBJECT_DIR="$(cd "$(dirname "$0")/../validation-subject" && pwd)"
DT_DIR="$SUBJECT_DIR/.dtfixingtools"

P="com.example.PolluterTest.pollute"
I="com.example.InnocentTest.innocent"
V="com.example.VictimTest.victim"

rm -rf "$DT_DIR"
mkdir -p "$DT_DIR/detection-results/random-class-method"
mkdir -p "$DT_DIR/test-runs/results"

# --- run result files (format understood by DetectionResultsLoader) ---

cat > "$DT_DIR/test-runs/results/run0" <<EOF
{
  "id": "run0",
  "testOrder": ["$P", "$I", "$V"],
  "results": {
    "$P": {"name": "$P", "result": "PASS", "time": 0.001, "stackTrace": []},
    "$I": {"name": "$I", "result": "PASS", "time": 0.001, "stackTrace": []},
    "$V": {"name": "$V", "result": "ERROR", "time": 0.002, "stackTrace": ["at com.example.VictimTest.victim(VictimTest.java:8)"]}
  }
}
EOF

cat > "$DT_DIR/test-runs/results/run1" <<EOF
{
  "id": "run1",
  "testOrder": ["$I", "$P", "$V"],
  "results": {
    "$I": {"name": "$I", "result": "PASS", "time": 0.001, "stackTrace": []},
    "$P": {"name": "$P", "result": "PASS", "time": 0.001, "stackTrace": []},
    "$V": {"name": "$V", "result": "ERROR", "time": 0.002, "stackTrace": ["at com.example.VictimTest.victim(VictimTest.java:8)"]}
  }
}
EOF

# --- detection round files (read by DetectionResultsLoader for RankFO scoring) ---

cat > "$DT_DIR/detection-results/random-class-method/round0.json" <<EOF
{"testRunIds": ["run0"]}
EOF

cat > "$DT_DIR/detection-results/random-class-method/round1.json" <<EOF
{"testRunIds": ["run1"]}
EOF

# --- flaky-lists.json (read by MinimizerMojo via DependentTestList.fromFile) ---
# intended: the passing order (VictimTest alone passes)
# revealed: the failing order (VictimTest fails when run after PolluterTest)

cat > "$DT_DIR/detection-results/flaky-lists.json" <<EOF
{
  "dts": [
    {
      "name": "$V",
      "intended": {
        "order": [],
        "result": "PASS",
        "testRunId": "intended-isolation"
      },
      "revealed": {
        "order": ["$P", "$I", "$V"],
        "result": "ERROR",
        "testRunId": "run0"
      },
      "type": "OD"
    }
  ]
}
EOF

echo "Fixtures written to $DT_DIR"
echo ""
echo "Expected RankFO PLUS_ONE ranking: [$P, $I]"
echo "Expected minimize output: polluters=[\"$P\"]"
