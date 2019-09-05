# iDFlakies

This repository contains tools for detecting/classifying flaky tests.

# Quickstart

The main script is located in `scripts/docker/create_and_run_dockers.sh`.
To use the script, you will need a `csv` file containing the GitHub URL and SHA that you wish to run iDFlakies on, one per line. This tool also requires [cloc](https://github.com/AlDanial/cloc) to be installed.

For Mac users, [gnu-sed](https://www.gnu.org/software/sed/) must be installed. There is also a Mac version of the main script located in `scripts/docker/create_and_run_dockers_mac.sh`

For example:
```
https://github.com/bonnyfone/vectalign,3393c5ee3440a48d5e7cec04bb6e2f0da532ba51
```

Once you have created the csv file, you can simply run:

```
bash create_and_run_dockers.sh <path to csv> <round num> <timeout (seconds)>
```

The output is explained in depth in `scripts/README.md`

# Cite

If you use iDFlakies, please cite our corresponding [ICST paper](http://winglam2.web.engr.illinois.edu/publications/2019/LamETAL19iDFlakies.pdf):
```
@inproceedings{LamETAL2019ICST,
    author    = {Lam, Wing and Oei, Reed and Shi, August and Marinov, Darko and Xie, Tao},
    title     = {{iDFlakies}: {A} Framework for Detecting and Partially Classifying Flaky Tests},
    booktitle = {12th {IEEE} Conference on Software Testing, Validation and Verification, {ICST} 2019, Xi'an, China, April 22-27, 2019},
    pages     = {312--322},
    year      = {2019},
}
```

Additional information about the projects analyzed in the paper can be found [here](https://sites.google.com/view/flakytestdataset/home)
