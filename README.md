# iDFlakies

This repository contains tools for detecting/classifying flaky tests.

# Quickstart

## Incorporating iDFlakies into Maven project

After building the plugin, you can add the plugin to a Maven project by modifying the pom.xml.

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

Run the following command on the Maven project:
```
mvn testrunner:testplugin
```

## Running experiments

The main script is located in `scripts/docker/create_and_run_dockers.sh`.
You will need Docker running to use the script.
To use the script, you will need a `csv` file containing the GitHub URL and SHA that you wish to run iDFlakies on, one per line.

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
