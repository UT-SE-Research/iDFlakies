# iDFlakies

This repository contains tools for detecting/classifying flaky tests.

# Quickstart

## Running iDFlakies tool on a Maven project

After building and installing the plugin, one can add the plugin to a Maven project by modifying its pom.xml with the following.

```xml
<build>
    ...
    <plugins>
        ...
        <plugin>
            <groupId>edu.illinois.cs</groupId>
            <artifactId>testrunner-maven-plugin</artifactId>
            <version>1.0.1</version>
            <dependencies>
                <dependency>
                    <groupId>edu.illinois.cs</groupId>
                    <artifactId>idflakies</artifactId>
                    <version>1.0.1-SNAPSHOT</version>
                </dependency>
            </dependencies>
            <configuration>
                <className>edu.illinois.cs.dt.tools.detection.DetectorPlugin</className>
            </configuration>
        </plugin>
    </plugins>
</build>
```

One can then run iDFlakies on the Maven project with the following command.
```
mvn testrunner:testplugin -Ddetector.detector_type=random-class-method -Ddt.randomize.rounds=10
```

iDFlakies configuration options:
* ```detector.detector_type``` - Configurations of iDFlakies as described on pages 3 and 4 of our [paper](http://winglam2.web.engr.illinois.edu/publications/2019/LamETAL19iDFlakies.pdf)
* ```dt.randomize.rounds``` - Number of times to run the test suite


## Running iDFlakies framework

The main script is located in `scripts/docker/create_and_run_dockers.sh`.
To use the script, you will need Docker installed and a `csv` file containing the GitHub URL and SHA that you wish to run iDFlakies on with one subject per line.

For example:
```
https://github.com/bonnyfone/vectalign,3393c5ee3440a48d5e7cec04bb6e2f0da532ba51
```

Once you have created the csv file, you can simply run:

```
bash create_and_run_dockers.sh <path to csv> <round num> <timeout (seconds)>
```

The script creates a new Docker image for each project/SHA, if one does not already exist.
Otherwise, the script will reuse the existing Docker image for the project.

The output of iDFlakies is explained in depth in `scripts/README.md` and the csv files used to evaluate iDFlakies are in `scripts/docker/icst-dataset`.


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
