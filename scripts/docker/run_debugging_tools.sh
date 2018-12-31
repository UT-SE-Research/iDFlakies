#!/usr/bin/env bash

echo "*******************REED************************"
echo "Starting run_debugging_tools.sh"

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

# Setup prolog stuff
cd /home/awshi2/dt-fixing-tools/scripts/
./setup

# Set environment up, just in case
source ~/.bashrc

# Incorporate tooling into the project, using Java XML parsing
# TODO: Update the modify-script to allow adding other classes. This would allow us to simplify the line below that calls the testrunner
cd /home/awshi2/${slug}
/home/awshi2/dt-fixing-tools/scripts/docker/pom-modify/modify-project.sh .

echo "*******************REED************************"
echo "Downloading known dt-lists.json for ${slug}"
bash /home/awshi2/dt-fixing-tools/scripts/docker/download-dt-lists.sh

# Run the plugin, get module test times
echo "*******************REED************************"
echo "Running the debugging tools"
date

timeout ${timeout}s /home/awshi2/apache-maven/bin/mvn testrunner:testplugin -Ddiagnosis.run_detection=false -Denforcer.skip=true -Drat.skip=true -Dtestplugin.className=edu.illinois.cs.dt.tools.diagnosis.DiagnoserPlugin -fn -B -e |& tee diagnosis.log

# Gather the results, put them up top
RESULTSDIR=/home/awshi2/output/
mkdir -p ${RESULTSDIR}
/home/awshi2/dt-fixing-tools/scripts/gather-results $(pwd) ${RESULTSDIR}
mv diagnosis.log ${RESULTSDIR}
mv /home/awshi2/mvn-test.log ${RESULTSDIR}
mv /home/awshi2/mvn-test-time.log ${RESULTSDIR}

echo "*******************REED************************"
echo "Finished run_debugging_tools.sh"
date

