#!/usr/bin/env python3
"""Generates .dtfixingtools/ fixtures for validation-subject: 4 non-victim tests
(polluter, 2 independents, cleaner) x 10 randomly-sampled orderings (out of the 4! = 24
possible), with the victim's expected result computed from the actual pollution semantics
(SharedState.polluted), not hand-typed.
"""
import itertools
import json
import random
import shutil
from pathlib import Path

P = "com.example.PolluterTest.pollute"
I1 = "com.example.InnocentTest.innocent"
I2 = "com.example.Innocent2Test.innocent2"
C = "com.example.CleanerTest.clean"
V = "com.example.VictimTest.victim"
NON_VICTIM = [P, I1, I2, C]

SUBJECT_DIR = Path(__file__).resolve().parent.parent.parent / "validation-subject"
DT_DIR = SUBJECT_DIR / ".dtfixingtools"


def victim_result(prefix_order):
    """Mirrors SharedState.polluted: PolluterTest sets it true, CleanerTest resets it
    false, the two Innocent tests are no-ops. Victim errors iff it's still polluted."""
    polluted = False
    for t in prefix_order:
        if t == P:
            polluted = True
        elif t == C:
            polluted = False
    return "ERROR" if polluted else "PASS"


def write_run(run_id, order, results_dir):
    results = {}
    for t in order[:-1]:
        results[t] = {"name": t, "result": "PASS", "time": 0.001, "stackTrace": []}
    victim_r = victim_result(order[:-1])
    results[V] = {
        "name": V, "result": victim_r, "time": 0.002,
        "stackTrace": [] if victim_r == "PASS" else
            ["at com.example.VictimTest.victim(VictimTest.java:8)"],
    }
    (results_dir / run_id).write_text(json.dumps(
        {"id": run_id, "testOrder": order, "results": results}, indent=2))
    return victim_r


def main():
    if DT_DIR.exists():
        shutil.rmtree(DT_DIR)
    round_dir = DT_DIR / "detection-results" / "random-class-method"
    results_dir = DT_DIR / "test-runs" / "results"
    round_dir.mkdir(parents=True)
    results_dir.mkdir(parents=True)

    random.seed(42)  # reproducible sample, not a fresh-random set every generation
    all_perms = list(itertools.permutations(NON_VICTIM))
    assert len(all_perms) == 24
    sampled = random.sample(all_perms, 10)

    run_ids = []
    intended = None  # first PASSing order encountered
    revealed = None  # first ERROR-ing order encountered

    for i, perm in enumerate(sampled):
        order = list(perm) + [V]
        run_id = f"run{i}"
        result = write_run(run_id, order, results_dir)
        run_ids.append(run_id)

        (round_dir / f"round{i}.json").write_text(
            json.dumps({"testRunIds": [run_id]}, indent=2))

        if result == "PASS" and intended is None:
            intended = {"order": order, "result": "PASS", "testRunId": run_id}
        if result == "ERROR" and revealed is None:
            revealed = {"order": order, "result": "ERROR", "testRunId": run_id}

    assert intended is not None, "no PASSing order sampled -- reroll the seed/sample size"
    assert revealed is not None, "no ERROR-ing order sampled -- reroll the seed/sample size"

    flaky_lists = {"dts": [{
        "name": V, "type": "OD",
        "intended": intended,
        "revealed": revealed,
    }]}
    (DT_DIR / "detection-results" / "flaky-lists.json").write_text(
        json.dumps(flaky_lists, indent=2))

    print(f"Generated {len(sampled)} orderings ({run_ids[0]}..{run_ids[-1]}) "
          f"to {DT_DIR}")
    print(f"  intended (PASS): {intended['testRunId']}")
    print(f"  revealed (ERROR): {revealed['testRunId']}")


if __name__ == "__main__":
    main()
