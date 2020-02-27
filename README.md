# iDFlakies

This repository contains tools for detecting/classifying flaky tests.

# Quickstart

## Using iDFlakies on a Maven project

One may use iDFlakies on a specific Maven project by getting it from Maven Central or building it themselves.


### Automatically setting up the pom.xml for iDFlakies

Run the following command to automatically setup the pom.xml for iDFlakies.


```shell
bash pom-modify/modify-project.sh path_to_maven_project
```

By default, modify-project.sh will use the version of iDFlakies from Maven Central. If you wish to use
the version of iDFlakies built locally, you can run the following instead. 

```shell
bash pom-modify/modify-project.sh path_to_maven_project 1.0.2-SNAPSHOT
```

### Manually setting up the pom.xml for iDFlakies

Copy the following plugin into the Maven project's pom.xml.
You do not need to perform this step if you have already completed the instructions
in [Automatically setting up the pom.xml for iDFlakies](#automatically-setting-up-the-pomxml-for-idflakies).

```xml
<build>
    ...
    <plugins>
        ...
        <plugin>
            <groupId>edu.illinois.cs</groupId>
            <artifactId>testrunner-maven-plugin</artifactId>
            <version>1.0</version>
            <dependencies>
                <dependency>
                    <groupId>edu.illinois.cs</groupId>
                    <artifactId>idflakies</artifactId>
                    <!-- Use iDFlakies from Maven Central -->
                    <version>1.0.1</version>
                    <!-- Use the following version instead if you build iDFlakies locally and want to use the locally built version. -->
                    <!-- <version>1.0.2-SNAPSHOT</version> -->
                </dependency>
            </dependencies>
            <configuration>
                <className>edu.illinois.cs.dt.tools.detection.DetectorPlugin</className>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Running iDFlakies

Once iDFlakies is added to a Maven project, one can then run iDFlakies on the project with the following command.

```shell
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
