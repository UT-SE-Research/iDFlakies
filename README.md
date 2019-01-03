# iDFlakies

This repository contains tools for detecting/classifying flaky tests.

# Quickstart

The main script is located in `scripts/docker/create_and_run_dockers.sh`.
To use the script, you will need a `csv` file containing the GitHub URL and SHA that you wish to run iDFlakies on, one per line.

For example:
```
https://github.com/bonnyfone/vectalign,3393c5ee3440a48d5e7cec04bb6e2f0da532ba51
```

Once you have created the csv file, you can simply run:

```
bash create_and_run_dockers.sh <path to csv> <round num> <timeout (seconds)>
```

The output is explained in depth in `scripts/README.md`

