#!/usr/bin/env python3
"""Verifies one RankFO strategy run against validation-subject:
  1. rankfo-scores/<HEURISTIC>/*.json's score for EVERY candidate (polluter, both
     independents, cleaner) matches an independently-computed expected value -- not just
     that the polluter's name shows up somewhere in the ranking.
  2. minimized/<STRATEGY>/*.json's accepted polluter and flakyClass match ground truth.
Expected scores are computed here from scratch against the paper's formulas, reading the
same raw fixture JSON RankFOScorer reads -- this script does not call into the Java code.
"""
import argparse
import json
import sys
from pathlib import Path

SUBJECT_DIR = Path(__file__).resolve().parent.parent.parent / "validation-subject"
DT_DIR = SUBJECT_DIR / ".dtfixingtools"
V = "com.example.VictimTest.victim"
EXPECTED_POLLUTER = "com.example.PolluterTest.pollute"
TOLERANCE = 1e-9

STRATEGY_TO_HEURISTIC = {
    "RANKFO_PLUS_ONE": "PLUS_ONE",
    "RANKFO_METHODS": "METHODS",
    "RANKFO_DISTANCE_D": "DISTANCE",
    "RANKFO_COMBINED_P1_D": "COMBINED_PLUS_ONE_DISTANCE",
    "RANKFO_COMBINED_M_D": "COMBINED_METHODS_DISTANCE",
}


def load_orderings():
    round_dir = DT_DIR / "detection-results" / "random-class-method"
    results_dir = DT_DIR / "test-runs" / "results"
    round_files = sorted(round_dir.glob("round*.json"),
                          key=lambda p: int(p.stem.replace("round", "")))
    orderings = []
    for rf in round_files:
        for run_id in json.loads(rf.read_text())["testRunIds"]:
            orderings.append(json.loads((results_dir / run_id).read_text()))
    return orderings


def score_delta(heuristic, relevant, count, dist):
    sign = 1.0 if relevant else -1.0
    if heuristic in ("PLUS_ONE", "COMBINED_PLUS_ONE_DISTANCE"):
        return sign
    if heuristic in ("METHODS", "COMBINED_METHODS_DISTANCE"):
        return 0.0 if count == 0 else sign * (1.0 / count)
    if heuristic == "DISTANCE":
        return sign * (1.0 / max(1, dist))
    raise ValueError(heuristic)


def expected_scores(heuristic, orderings):
    candidates = set()
    for o in orderings:
        order = o["testOrder"]
        if V in order:
            candidates.update(order[:order.index(V)])

    current = {c: 0.0 for c in candidates}
    polluter_score = {c: 0.0 for c in candidates}
    non_polluter_score = {c: 0.0 for c in candidates}

    for o in orderings:
        order = o["testOrder"]
        if V not in order:
            continue
        victim_result = o["results"].get(V, {}).get("result")
        if victim_result is None:
            continue

        prev = dict(current)
        sub_order = order[:order.index(V)]
        relevant = victim_result in ("FAILURE", "ERROR")
        count = len(sub_order)
        for idx, c in enumerate(sub_order):
            if c not in current:
                continue
            dist = count - idx
            current[c] += score_delta(heuristic, relevant, count, dist)

        # Diff every order (including order 0) against the zero baseline -- matches the
        # i>0-gate fix in RankFOScorer.java.
        for c in candidates:
            diff = abs(current[c] - prev[c])
            if current[c] > prev[c]:
                polluter_score[c] += diff
            elif current[c] < prev[c]:
                non_polluter_score[c] += diff

    return polluter_score, non_polluter_score


def check_scores(strategy, heuristic, orderings):
    exp_polluter, exp_non_polluter = expected_scores(heuristic, orderings)
    score_file = DT_DIR / "rankfo-scores" / heuristic / f"{V}-{heuristic}-VICTIM_POLLUTER.json"

    if not score_file.exists():
        print(f"FAIL [{strategy}]: expected score file not found: {score_file}")
        return False

    actual_by_name = {c["testName"]: c
                       for c in json.loads(score_file.read_text())["candidates"]}

    ok = True
    for name in sorted(exp_polluter):
        short = name.rsplit(".", 1)[-1]
        if name not in actual_by_name:
            print(f"FAIL [{strategy}] {short}: missing from {score_file.name}")
            ok = False
            continue
        a_p = actual_by_name[name]["polluterScore"]
        a_np = actual_by_name[name]["nonPolluterScore"]
        e_p, e_np = exp_polluter[name], exp_non_polluter[name]
        if abs(a_p - e_p) > TOLERANCE or abs(a_np - e_np) > TOLERANCE:
            print(f"FAIL [{strategy}] {short}: actual=({a_p:.6f},{a_np:.6f}) "
                  f"expected=({e_p:.6f},{e_np:.6f})")
            ok = False
        else:
            print(f"  OK  [{strategy}] {short:12s} polluterScore={a_p:+.6f} "
                  f"nonPolluterScore={a_np:+.6f}")
    return ok


def check_minimized(strategy):
    minimized_dir = DT_DIR / "minimized" / strategy
    files = list(minimized_dir.glob("*.json")) if minimized_dir.exists() else []
    if not files:
        print(f"FAIL [{strategy}]: no minimized output in {minimized_dir}")
        return False

    result = json.loads(files[0].read_text())
    deps = result.get("polluters", [{}])[0].get("deps", []) if result.get("polluters") else []
    flaky_class = result.get("flakyClass")

    ok = True
    if deps != [EXPECTED_POLLUTER]:
        print(f"FAIL [{strategy}]: minimized polluter={deps}, expected [{EXPECTED_POLLUTER}]")
        ok = False
    if flaky_class != "OD":
        print(f"FAIL [{strategy}]: flakyClass={flaky_class}, expected OD")
        ok = False
    if ok:
        print(f"  OK  [{strategy}] minimized polluter="
              f"{EXPECTED_POLLUTER.rsplit('.',1)[-1]} flakyClass=OD")
    return ok


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--strategy", required=True, choices=STRATEGY_TO_HEURISTIC.keys())
    args = parser.parse_args()
    heuristic = STRATEGY_TO_HEURISTIC[args.strategy]

    orderings = load_orderings()
    scores_ok = check_scores(args.strategy, heuristic, orderings)
    minimized_ok = check_minimized(args.strategy)

    sys.exit(0 if (scores_ok and minimized_ok) else 1)


if __name__ == "__main__":
    main()
