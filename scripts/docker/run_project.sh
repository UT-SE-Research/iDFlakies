#!/bin/bash

SCRIPT_USERNAME="idflakies"
TOOL_REPO="iDFlakies"

git rev-parse HEAD

date

# This script is run inside the Docker image, for single experiment (one project)
# Should only be invoked by the run_experiment.sh script

if [[ $1 == "" ]] || [[ $2 == "" ]] || [[ $3 == "" ]]; then
    echo "arg1 - GitHub SLUG"
    echo "arg2 - Number of rounds"
    echo "arg3 - Timeout in seconds"
    exit
fi

slug=$1
rounds=$2
timeout=$3

iDFlakiesVersion=1.2.0-SNAPSHOT

# Setup prolog stuff
cd "/home/$SCRIPT_USERNAME/$TOOL_REPO/scripts/"
./setup

# Incorporate tooling into the project, using Java XML parsing
cd "/home/$SCRIPT_USERNAME/${slug}"
/home/$SCRIPT_USERNAME/$TOOL_REPO/pom-modify/modify-project.sh . $iDFlakiesVersion

# Run the plugin, get module test times
echo "*******************iDFLAKIES************************"
echo "Running testplugin for getting module test time"
date

modifiedslug=$(echo ${slug} | sed 's;/;.;' | tr '[:upper:]' '[:lower:]')

# Set global mvn options for skipping things
MVNOPTIONS="-Denforcer.skip=true -Drat.skip=true -Dmdep.analyze.skip=true -Dmaven.javadoc.skip=true"
IDF_OPTIONS="-Ddt.detector.original_order.all_must_pass=false -Ddetector.timeout=${timeout} -Ddt.randomize.rounds=${rounds} -fn -B -e -Ddt.cache.absolute.path=/Scratch/all-output/${modifiedslug}_output"

# Optional timeout... In practice our tools really shouldn't need 1hr to parse a project's surefire reports.
timeout 1h /home/$SCRIPT_USERNAME/apache-maven/bin/mvn testrunner:testplugin ${MVNOPTIONS} -Dtestplugin.className=edu.illinois.cs.dt.tools.utility.ModuleTestTimePlugin -fn -B -e -Ddt.cache.absolute.path=/Scratch/all-output/${modifiedslug}_output |& tee module_test_time.log


# Run the plugin, reversing the original order (reverse class and methods)
echo "*******************iDFLAKIES************************"
echo "Running testplugin for reversing the original order"
date

timeout ${timeout}s /home/$SCRIPT_USERNAME/apache-maven/bin/mvn testrunner:testplugin ${MVNOPTIONS} ${IDF_OPTIONS} -Ddetector.detector_type=reverse |& tee reverse_original.log


# Run the plugin, reversing the original order (reverse class)
echo "*******************iDFLAKIES************************"
echo "Running testplugin for reversing the class order"
date

timeout ${timeout}s /home/$SCRIPT_USERNAME/apache-maven/bin/mvn testrunner:testplugin ${MVNOPTIONS} ${IDF_OPTIONS} -Ddetector.detector_type=reverse-class |& tee reverse_class.log


# Run the plugin, original order
echo "*******************iDFLAKIES************************"
echo "Running testplugin for original"
date

timeout ${timeout}s /home/$SCRIPT_USERNAME/apache-maven/bin/mvn testrunner:testplugin ${MVNOPTIONS} ${IDF_OPTIONS} -Ddetector.detector_type=original |& tee original.log


# Run the plugin, random class first, method second
echo "*******************iDFLAKIES************************"
echo "Running testplugin for randomizemethods"
date

timeout ${timeout}s /home/$SCRIPT_USERNAME/apache-maven/bin/mvn testrunner:testplugin ${MVNOPTIONS} ${IDF_OPTIONS} |& tee random_class_method.log


# Run the plugin, random class only
echo "*******************iDFLAKIES************************"
echo "Running testplugin for randomizeclasses"
date

timeout ${timeout}s /home/$SCRIPT_USERNAME/apache-maven/bin/mvn testrunner:testplugin ${MVNOPTIONS} ${IDF_OPTIONS} -Ddetector.detector_type=random-class |& tee random_class.log

# Run the smart-shuffle (every test runs first and last)
echo "*******************iDFLAKIES************************"
echo "Running testplugin for smart-shuffle"
date

timeout ${timeout}s /home/$SCRIPT_USERNAME/apache-maven/bin/mvn testrunner:testplugin ${MVNOPTIONS} ${IDF_OPTIONS} -Ddetector.detector_type=smart-shuffle |& tee smart_shuffle.log

# Gather the results, put them up top
RESULTSDIR=/home/$SCRIPT_USERNAME/output/
mkdir -p ${RESULTSDIR}
/home/$SCRIPT_USERNAME/$TOOL_REPO/scripts/gather-results $(pwd) ${RESULTSDIR}
mv *.log ${RESULTSDIR}/

echo "*******************iDFLAKIES************************"
echo "Finished run_project.sh"
date

